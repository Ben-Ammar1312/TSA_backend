package org.example.tas_backend.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.tas_backend.dtos.StaffProfileDTO;
import org.example.tas_backend.services.StaffProfileService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/staff")
@RequiredArgsConstructor
public class StaffController {

    private final StaffProfileService service;

    /** Create-or-refresh local profile snapshot from Keycloak claims. */
    @PostMapping("/profile/init")
    public ResponseEntity<StaffProfileDTO> init(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(service.ensureFromJwt(jwt));
    }

    /** Get my profile (requires profile to exist). */
    @GetMapping("/profile")
    public ResponseEntity<StaffProfileDTO> get(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(service.getMine(jwt.getSubject()));
    }

    /** Patch my profile. Only mutable fields are updated. */
    @PatchMapping("/profile")
    public ResponseEntity<StaffProfileDTO> update(@AuthenticationPrincipal Jwt jwt,
                                                  @Valid @RequestBody StaffProfileDTO body) {
        return ResponseEntity.ok(service.updateMine(jwt.getSubject(), body));
    }

    /** Delete my profile. */
    @DeleteMapping("/profile")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal Jwt jwt) {
        service.deleteMine(jwt.getSubject());
        return ResponseEntity.noContent().build();
    }
}