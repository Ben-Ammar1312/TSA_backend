package org.example.tas_backend.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.tas_backend.dtos.StaffProfileDTO;
import org.example.tas_backend.dtos.StaffRegistrationRequestDTO;
import org.example.tas_backend.services.StaffRegistrationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/staff")
@RequiredArgsConstructor
public class StaffAdminController {

    private final StaffRegistrationService registrationService;

    @PostMapping("/register")
    public ResponseEntity<StaffProfileDTO> registerStaff(
            @Valid @RequestBody StaffRegistrationRequestDTO body) {

        StaffProfileDTO dto = registrationService.registerStaff(body);
        return ResponseEntity.ok(dto);
    }
}