package org.example.tas_backend.controllers;

import lombok.RequiredArgsConstructor;
import org.example.tas_backend.dtos.UserSummaryDTO;
import org.example.tas_backend.repos.StaffRepo;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.*;

import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class UserAdminController {

    private final Keycloak keycloak;
    private final StaffRepo staffRepo;

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

            // Try to read phone/job from Keycloak attributes; fall back to staff entity
            String phone = null;
            String jobTitle = null;
            if (u.getAttributes() != null) {
                var phones = u.getAttributes().get("phoneNumber");
                if (phones == null || phones.isEmpty()) {
                    phones = u.getAttributes().get("phone_number"); // common alt
                }
                if (phones != null && !phones.isEmpty()) {
                    phone = phones.get(0);
                }

                var jobTitles = u.getAttributes().get("jobTitle");
                if ((jobTitles == null || jobTitles.isEmpty()) && u.getAttributes().containsKey("job_title")) {
                    jobTitles = u.getAttributes().get("job_title");
                }
                if (jobTitles != null && !jobTitles.isEmpty()) {
                    jobTitle = jobTitles.get(0);
                }
            }
            var staffOpt = staffRepo.findByKeycloakSub(u.getId());
            if (staffOpt.isPresent()) {
                var staff = staffOpt.get();
                if (phone == null) phone = staff.getPhoneNumber();
                if (jobTitle == null) jobTitle = staff.getJobTitle();
            }

            return new UserSummaryDTO(
                    u.getId(),
                    u.getUsername(),
                    u.getEmail(),
                    u.getFirstName(),
                    u.getLastName(),
                    roles,
                    phone,
                    jobTitle
            );
        }).toList();
    }

    /**
     * Delete a Keycloak user by id and remove related staff record if present.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable String id) {
        var usersResource = keycloak.realm(realm).users();
        // Remove from Keycloak (idempotent-ish)
        Response kcResp = usersResource.delete(id);
        int status = kcResp.getStatus();
        if (status == 404 || status == 400) {
            // Already gone or invalid id; continue cleanup so UI stays idempotent
        } else if (status >= 300) {
            throw new ResponseStatusException(
                    HttpStatus.valueOf(status),
                    "Failed to delete Keycloak user, status " + status);
        }
        kcResp.close();

        // Clean up staff profile if it exists
        staffRepo.findIdByKeycloakSub(id).ifPresent(staffRepo::deleteById);

        return ResponseEntity.noContent().build();
    }
}
