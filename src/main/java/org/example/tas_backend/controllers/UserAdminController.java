package org.example.tas_backend.controllers;

import lombok.RequiredArgsConstructor;
import org.example.tas_backend.dtos.UserSummaryDTO;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class UserAdminController {

    private final Keycloak keycloak;

    @Value("${keycloak.realm}")
    private String realm;

    /**
     * List Keycloak users.
     *
     * @param role  optional realm role filter (e.g. "student", "staff", "admin").
     * @param first pagination start index (default 0).
     * @param max   page size (default 50).
     */
    @GetMapping
    public List<UserSummaryDTO> listUsers(
            @RequestParam(required = false) String role,
            @RequestParam(defaultValue = "0") int first,
            @RequestParam(defaultValue = "50") int max) {

        var usersResource = keycloak.realm(realm).users();

        // Basic paged listing
        List<UserRepresentation> users = usersResource.list(first, max);

        // Optional filter by realm role
        if (role != null && !role.isBlank()) {
            String roleName = role.trim();
            users = users.stream().filter(u -> {
                var roleMappings = usersResource.get(u.getId())
                        .roles()
                        .realmLevel()
                        .listAll();
                Set<String> names = roleMappings.stream()
                        .map(RoleRepresentation::getName)
                        .collect(Collectors.toSet());
                return names.contains(roleName);
            }).toList();
        }

        // Map to DTOs
        return users.stream().map(u -> {
            var roleMappings = usersResource.get(u.getId())
                    .roles()
                    .realmLevel()
                    .listAll();
            List<String> roles = roleMappings.stream()
                    .map(RoleRepresentation::getName)
                    .toList();

            return new UserSummaryDTO(
                    u.getId(),
                    u.getUsername(),
                    u.getEmail(),
                    u.getFirstName(),
                    u.getLastName(),
                    roles
            );
        }).toList();
    }
}