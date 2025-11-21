package org.example.tas_backend.services;

import jakarta.transaction.Transactional;
import org.example.tas_backend.dtos.StaffProfileDTO;
import org.example.tas_backend.entities.Audit;
import org.example.tas_backend.entities.Staff;
import org.example.tas_backend.enums.Gender;
import org.example.tas_backend.repos.StaffRepo;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.NoSuchElementException;

@Service
public class StaffProfileService {

    private final StaffRepo repo;

    public StaffProfileService(StaffRepo repo) {
        this.repo = repo;
    }

    /** Create-or-refresh snapshot from Keycloak JWT (firstName, lastName, email). */
    @Transactional
    public StaffProfileDTO ensureFromJwt(Jwt jwt) {
        String sub   = jwt.getSubject();
        String given = jwt.getClaimAsString("given_name");
        String fam   = jwt.getClaimAsString("family_name");
        String mail  = jwt.getClaimAsString("email");

        Staff s = repo.findByKeycloakSub(sub).orElseGet(() -> {
            Staff n = new Staff();
            n.setKeycloakSub(sub);
            return n;
        });

        if (given != null) s.setFirstName(given);
        if (fam   != null) s.setLastName(fam);
        if (mail  != null) s.setEmail(mail);

        if (s.getAudit() == null) s.setAudit(new Audit());
        if (s.getId() == null) {
            s.getAudit().setCreatedBy(sub);
        }
        s.getAudit().setUpdatedBy(sub);

        s = repo.save(s);
        return toDto(s);
    }

    @Transactional
    public StaffProfileDTO getMine(String sub) {
        return toDto(findBySub(sub));
    }

    @Transactional
    public StaffProfileDTO updateMine(String sub, StaffProfileDTO dto) {
        Staff s = findBySub(sub);

        if (dto.phoneNumber() != null) s.setPhoneNumber(dto.phoneNumber());
        if (dto.jobTitle()    != null) s.setJobTitle(dto.jobTitle());
        if (dto.department()  != null) s.setDepartment(dto.department());
        if (dto.nationalID()  != null) s.setNationalID(dto.nationalID());

        if (dto.gender() != null) s.setGender(Gender.valueOf(dto.gender().name()));

        if (dto.dateOfBirth() != null) s.setDateOfBirth(dto.dateOfBirth());

        s = repo.save(s);
        return toDto(s);
    }

    @Transactional
    public void deleteMine(String sub) {
        repo.findByKeycloakSub(sub).ifPresent(repo::delete);
    }

    // ---------- helpers ----------

    private Staff findBySub(String sub) {
        return repo.findByKeycloakSub(sub)
                .orElseThrow(() -> new NoSuchElementException("staff profile not found"));
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