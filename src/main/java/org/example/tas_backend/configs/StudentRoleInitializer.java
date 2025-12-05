package org.example.tas_backend.configs;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RolesResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Ensures self-registered users get the "student" realm role:
 *  - create the role if missing
 *  - add it to the realm's default composite role (applies to future registrations)
 *  - upgrade existing non-admin/non-staff users that lack the student role
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StudentRoleInitializer implements ApplicationRunner {

    private final Keycloak keycloak;

    @Value("${keycloak.realm}")
    private String realm;

    @Override
    public void run(ApplicationArguments args) {
        try {
            var realmResource = keycloak.realm(realm);
            var rolesResource = realmResource.roles();
            var usersResource = realmResource.users();

            RoleRepresentation studentRole = ensureRoleExists(rolesResource, "student");
            ensureDefaultRoleContainsStudent(rolesResource, studentRole);

            int upgraded = grantStudentToExistingUsers(usersResource, studentRole);
            log.info("Student role bootstrap complete: upgraded {} users; default role enforced", upgraded);
        } catch (Exception e) {
            log.warn("Unable to enforce student default role: {}", e.getMessage());
        }
    }

    private RoleRepresentation ensureRoleExists(RolesResource roles, String roleName) {
        try {
            return roles.get(roleName).toRepresentation();
        } catch (Exception ignored) {
            // create if missing
            var rep = new RoleRepresentation();
            rep.setName(roleName);
            roles.create(rep); // in KC 26.x this returns void
            return roles.get(roleName).toRepresentation();
        }
    }

    private void ensureDefaultRoleContainsStudent(RolesResource roles, RoleRepresentation studentRole) {
        // Realm default composite role name is "default-roles-<realm>"
        String defaultRoleName = "default-roles-" + realm;
        var defaultRole = roles.get(defaultRoleName);
        if (defaultRole == null) {
            log.warn("Default realm role {} not found; new users may miss default roles", defaultRoleName);
            return;
        }
        boolean hasStudent = defaultRole.getRoleComposites().stream()
                .anyMatch(r -> studentRole.getName().equalsIgnoreCase(r.getName()));
        if (!hasStudent) {
            defaultRole.addComposites(Collections.singletonList(studentRole));
            log.info("Added '{}' to default roles for realm {}", studentRole.getName(), realm);
        }
    }

    private int grantStudentToExistingUsers(org.keycloak.admin.client.resource.UsersResource usersResource,
                                            RoleRepresentation studentRole) {
        int first = 0;
        int pageSize = 100;
        int upgraded = 0;

        List<UserRepresentation> page;
        do {
            page = usersResource.list(first, pageSize);
            for (UserRepresentation user : page) {
                if (isServiceAccount(user)) continue;

                UserResource userResource = usersResource.get(user.getId());
                Set<String> realmRoles = userResource.roles()
                        .realmLevel()
                        .listAll()
                        .stream()
                        .map(RoleRepresentation::getName)
                        .collect(Collectors.toSet());

                if (realmRoles.contains("student") || realmRoles.contains("admin") || realmRoles.contains("staff")) {
                    continue; // already has a privileged role
                }

                userResource.roles().realmLevel().add(Collections.singletonList(studentRole));
                upgraded++;
            }
            first += pageSize;
        } while (!page.isEmpty());

        return upgraded;
    }

    private boolean isServiceAccount(UserRepresentation user) {
        String username = user.getUsername();
        return username != null && username.startsWith("service-account-");
    }
}
