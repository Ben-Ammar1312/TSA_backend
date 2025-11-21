package org.example.tas_backend.controllers;
import jakarta.validation.Valid;
import org.example.tas_backend.dtos.ApplicationSubmitDTO;
import org.example.tas_backend.dtos.StudentApplicantProfileDTO;
import org.example.tas_backend.entities.Application;
import org.example.tas_backend.services.ApplicationSubmitService;
import org.example.tas_backend.services.StudentApplicantService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/student/")
public class StudentApplicantController {

    private final StudentApplicantService service;
    private final ApplicationSubmitService submitService;

    public StudentApplicantController(StudentApplicantService service, ApplicationSubmitService submitService) {
        this.service = service;
        this.submitService = submitService;
    }

    /** Create-or-refresh local profile snapshot from Keycloak claims. */
    @PostMapping("/profile/init")
    public ResponseEntity<StudentApplicantProfileDTO> init(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(service.ensureFromJwt(jwt));
    }

    /** Get my profile – will auto-create/sync from Keycloak if missing. */
    @GetMapping("/profile")
    public ResponseEntity<StudentApplicantProfileDTO> get(@AuthenticationPrincipal Jwt jwt) {
        // EASY OPTION: create if not exists; refresh basic fields if it does
        return ResponseEntity.ok(service.ensureFromJwt(jwt));
    }

    /** Patch my profile. Only mutable fields are updated. */
    @PatchMapping("/profile")
    public ResponseEntity<StudentApplicantProfileDTO> update(@AuthenticationPrincipal Jwt jwt,
                                                             @Valid @RequestBody StudentApplicantProfileDTO body) {
        return ResponseEntity.ok(service.updateMine(jwt.getSubject(), body));
    }

    /** Delete my profile. */
    @DeleteMapping("/profile")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal Jwt jwt) {
        service.deleteMine(jwt.getSubject());
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/apply")
    public ResponseEntity<Long> apply(
            @AuthenticationPrincipal Jwt jwt,
            @RequestPart("data") @Valid ApplicationSubmitDTO data,
            @RequestPart("files") List<MultipartFile> files
    ) throws IOException {

        String sub = jwt.getSubject();
        Application app = submitService.submit(sub, data, files);
        return ResponseEntity.ok(app.getId());
    }
}
