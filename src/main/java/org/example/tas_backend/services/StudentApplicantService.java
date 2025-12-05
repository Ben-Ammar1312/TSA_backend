package org.example.tas_backend.services;

import jakarta.transaction.Transactional;
import org.example.tas_backend.dtos.AddressDTO;
import org.example.tas_backend.entities.Address;
import org.example.tas_backend.entities.Audit;
import org.example.tas_backend.entities.StudentApplicant;
import org.example.tas_backend.enums.Gender;
import org.example.tas_backend.entities.Application;
import org.example.tas_backend.entities.Document;
import org.example.tas_backend.repos.ApplicationRepo;
import org.example.tas_backend.repos.DocumentRepo;
import org.example.tas_backend.repos.StudentApplicantRepo;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.RoleRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.example.tas_backend.dtos.StudentApplicantProfileDTO;
import org.example.tas_backend.dtos.DocumentInfoDTO;
import lombok.extern.slf4j.Slf4j;


import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class StudentApplicantService {

    private final StudentApplicantRepo repo;
    private final ApplicationRepo applicationRepo;
    private final DocumentRepo documentRepo;
    private final Keycloak keycloak;
    private final String realm;
    @Value("${storage.upload-root:uploads}")
    private String uploadRoot;


    public StudentApplicantService(StudentApplicantRepo repo,
                                   ApplicationRepo applicationRepo,
                                   DocumentRepo documentRepo,
                                   Keycloak keycloak,
                                   @Value("${keycloak.realm}") String realm) {
        this.repo = repo;
        this.applicationRepo = applicationRepo;
        this.documentRepo = documentRepo;
        this.keycloak = keycloak;
        this.realm = realm;
    }

    /** Create-or-refresh snapshot from Keycloak JWT (firstName, lastName, email). */
    @Transactional
    public StudentApplicantProfileDTO ensureFromJwt(Jwt jwt) {
        String sub   = jwt.getSubject();
        String given = jwt.getClaimAsString("given_name");
        String fam   = jwt.getClaimAsString("family_name");
        String mail  = jwt.getClaimAsString("email");

        StudentApplicant sa = repo.findByKeycloakSub(sub).orElseGet(() -> {
            var n = new StudentApplicant();
            n.setKeycloakSub(sub);
            return n;
        });

        // snapshot only, user can later override via profile form
        if (given != null) sa.setFirstName(given);
        if (fam   != null) sa.setLastName(fam);
        if (mail  != null) sa.setEmail(mail);
        if (sa.getAudit() == null) sa.setAudit(new Audit());
        if (sa.getId() == null) {
            sa.getAudit().setCreatedBy(sub);
        }
        sa.getAudit().setUpdatedBy(sub);

        sa = repo.save(sa);

        // make sure realm role student exists for this user (unless admin/staff)
        ensureStudentRole(sub);

        return toDto(sa);
    }

    @Transactional
    public StudentApplicantProfileDTO getMine(String sub) {
        return toDto(findBySub(sub));
    }

    @Transactional
    public StudentApplicantProfileDTO updateMine(String sub, StudentApplicantProfileDTO dto) {
        var sa = findBySub(sub);

        if (dto.phoneNumber() != null) sa.setPhoneNumber(dto.phoneNumber());
        if (dto.dateOfBirth() != null) sa.setDateOfBirth(dto.dateOfBirth());
        if (dto.nationalID() != null)  sa.setNationalID(dto.nationalID());
        if (dto.nationality() != null) sa.setNationality(dto.nationality());
        if (dto.residence() != null)   sa.setResidence(dto.residence());
        if (dto.visaStatus() != null)  sa.setVisaStatus(dto.visaStatus());
        if (dto.photoDataUrl() != null) sa.setPhotoData(dto.photoDataUrl());

        if (dto.gender() != null) {
            // tolerate lowercase from clients
            sa.setGender(Gender.valueOf(dto.gender().toUpperCase()));
        }

        if (dto.address() != null) {
            sa.setAddress(mergeAddress(sa.getAddress(), dto.address()));
        }

        sa = repo.save(sa);
        return toDto(sa);
    }

    @Transactional
    public void deleteMine(String sub) {
        repo.findByKeycloakSub(sub).ifPresent(repo::delete);
    }

    // ---------- helpers ----------

    private StudentApplicant findBySub(String sub) {
        return repo.findByKeycloakSub(sub)
                .orElseThrow(() -> new NoSuchElementException("profile not found"));
    }

    private StudentApplicantProfileDTO toDto(StudentApplicant e) {
        Application latestApp = applicationRepo.findTopByStudentOrderByIdDesc(e).orElse(null);
        var docs = latestApp != null ? documentRepo.findByApplication(latestApp) : java.util.List.<Document>of();

        return new StudentApplicantProfileDTO(
            e.getId(),
            e.getKeycloakSub(),
            e.getFirstName(),
            e.getLastName(),
            e.getEmail(),
            e.getPhoneNumber(),
            e.getGender() == null ? null : e.getGender().name(),
            e.getDateOfBirth(),
            e.getNationalID(),
            toDto(e.getAddress()),
            e.getNationality(),
            e.getResidence(),
            e.getVisaStatus(),
            latestApp != null ? latestApp.getLanguageLevel() : null,
            docs.stream().map(this::toPublicPath).toList(),
            docs.stream()
                    .map(this::toDocumentInfo)
                    .toList(),
            e.getPhotoData()
        );
    }

    private AddressDTO toDto(Address a) {
        if (a == null) return null;
        return new AddressDTO(
                a.getLine1(),
                a.getLine2(),
                a.getCity(),
                a.getRegion(),
                a.getPostalCode(),
                a.getCountry()
        );
    }

    private Address mergeAddress(Address current, AddressDTO dto) {
        Address a = current == null ? new Address() : current;
        if (dto.line1()          != null) a.setLine1(dto.line1());
        if (dto.line2()          != null) a.setLine2(dto.line2());
        if (dto.city()           != null) a.setCity(dto.city());
        if (dto.stateOrProvince()!= null) a.setRegion(dto.stateOrProvince());
        if (dto.postalCode()     != null) a.setPostalCode(dto.postalCode());
        if (dto.country()        != null) a.setCountry(dto.country());
        return a;
    }

    private String toPublicPath(Document doc) {
        if (doc == null) return null;
        String key = doc.getStorageKey();
        if (key == null) return null;
        String normalized = key.replace("\\", "/");
        if (normalized.contains(uploadRoot)) {
            int idx = normalized.indexOf(uploadRoot);
            return "/" + normalized.substring(idx);
        }
        return "/" + normalized;
    }

    private DocumentInfoDTO toDocumentInfo(Document doc) {
        String url = toPublicPath(doc);
        return new DocumentInfoDTO(
                url,
                doc.getFilename(),
                doc.getMimeType(),
                doc.getSizeBytes(),
                null
        );
    }

    /**
     * Adds realm role "student" to the user if they don't already have student/staff/admin.
     */
    private void ensureStudentRole(String keycloakSub) {
        try {
            var userResource = keycloak.realm(realm).users().get(keycloakSub);
            Set<String> roles = userResource.roles()
                    .realmLevel()
                    .listAll()
                    .stream()
                    .map(RoleRepresentation::getName)
                    .map(String::toLowerCase)
                    .collect(Collectors.toSet());

            if (roles.contains("admin") || roles.contains("staff") || roles.contains("student")) {
                return; // already has a meaningful realm role
            }

            var studentRole = keycloak.realm(realm).roles().get("student").toRepresentation();
            userResource.roles().realmLevel().add(java.util.Collections.singletonList(studentRole));
            log.info("Granted student role to user {}", keycloakSub);
        } catch (Exception e) {
            log.warn("Unable to ensure student role for {}: {}", keycloakSub, e.getMessage());
        }
    }
}
