package org.example.tas_backend.services;

import jakarta.transaction.Transactional;
import org.example.tas_backend.dtos.StaffProfileDTO;
import org.example.tas_backend.dtos.StaffRegistrationRequestDTO;
import org.example.tas_backend.entities.Audit;
import org.example.tas_backend.entities.Staff;
import org.example.tas_backend.enums.Gender;
import org.example.tas_backend.repos.StaffRepo;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import jakarta.ws.rs.core.Response;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Collections;

@Service
public class StaffRegistrationService {

    private final Keycloak keycloak;
    private final String realm;
    private final StaffRepo staffRepo;

    public StaffRegistrationService(Keycloak keycloak,
                                    @Value("${keycloak.realm}") String realm,
                                    StaffRepo staffRepo) {
        this.keycloak = keycloak;
        this.realm = realm;
        this.staffRepo = staffRepo;
    }

    @Transactional
    public StaffProfileDTO registerStaff(StaffRegistrationRequestDTO req) {
        // 1. Create user in Keycloak and get UUID
        String keycloakUserId = createUserInKeycloak(req);

        // 2. Assign realm role "staff" (Keycloak role must exist: 'staff')
        assignStaffRole(keycloakUserId);

        // 3. Create Staff entity in local DB
        Staff staff = new Staff();
        staff.setKeycloakSub(keycloakUserId);
        staff.setFirstName(req.firstName());
        staff.setLastName(req.lastName());
        staff.setEmail(req.email());
        staff.setPhoneNumber(req.phoneNumber());
        staff.setJobTitle(req.jobTitle());
        staff.setDepartment(req.department());
        staff.setNationalID(req.nationalID());

        if (req.gender() != null) {
            staff.setGender(req.gender());
        }

        if (req.dateOfBirth() != null && !req.dateOfBirth().isBlank()) {
            // expect ISO yyyy-MM-dd from frontend; adjust parsing if needed
            try {
                staff.setDateOfBirth(LocalDate.parse(req.dateOfBirth()));
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException("Invalid dateOfBirth format, expected yyyy-MM-dd");
            }
        }

        Audit audit = new Audit();
        audit.setCreatedBy(keycloakUserId);
        audit.setUpdatedBy(keycloakUserId);
        staff.setAudit(audit);

        staff = staffRepo.save(staff);

        return toDto(staff);
    }

    // ---------- helpers ----------

    private String createUserInKeycloak(StaffRegistrationRequestDTO req) {
        UserRepresentation user = new UserRepresentation();
        user.setUsername(req.username());
        user.setEmail(req.email());
        user.setFirstName(req.firstName());
        user.setLastName(req.lastName());
        user.setEnabled(true);

        Response response = keycloak.realm(realm)
                .users()
                .create(user);

        int status = response.getStatus();
        String kcBody = null;
        try {
            kcBody = response.readEntity(String.class);
        } catch (Exception ignored) {}
        if (status >= 300) {
            // Map common statuses to clearer API errors
            if (status == 409) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Username or email already exists in Keycloak.");
            }
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Failed to create user in Keycloak, status: " + status + (kcBody != null ? " body: " + kcBody : "")
            );
        }

        String userId = extractId(response);

        // Set password
        CredentialRepresentation passwordCred = new CredentialRepresentation();
        passwordCred.setTemporary(false);
        passwordCred.setType(CredentialRepresentation.PASSWORD);
        passwordCred.setValue(req.password());

        keycloak.realm(realm)
                .users()
                .get(userId)
                .resetPassword(passwordCred);

        return userId;
    }

    private void assignStaffRole(String userId) {
        // Realm role name in Keycloak must be "staff" → Spring sees ROLE_STAFF
        RoleRepresentation staffRole = keycloak.realm(realm)
                .roles()
                .get("staff")
                .toRepresentation();

        keycloak.realm(realm)
                .users()
                .get(userId)
                .roles()
                .realmLevel()
                .add(Collections.singletonList(staffRole));
    }

    private String extractId(Response response) {
        String location = response.getLocation().toString();
        return location.substring(location.lastIndexOf('/') + 1);
    }

    private StaffProfileDTO toDto(Staff e) {
        return new StaffProfileDTO(
                e.getId(),
                e.getKeycloakSub(),
                e.getFirstName(),
                e.getLastName(),
                e.getEmail(),
                e.getPhoneNumber(),
                e.getGender(),
                e.getDateOfBirth(),
                e.getNationalID(),
                e.getJobTitle(),
                e.getDepartment()
        );
    }
}
