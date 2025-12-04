package org.example.tas_backend.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import org.example.tas_backend.enums.Gender;

import java.time.LocalDate;

public record StaffProfileDTO(
        Long id,
        String keycloakSub,
        String firstName,
        String lastName,
        @Email String email,
        @Pattern(regexp = "^[+0-9 ()-]{6,}$", message = "invalid phone") String phoneNumber,
        Gender gender,
        @Past LocalDate dateOfBirth,
        String nationalID,
        String jobTitle,
        String department,
        String photoDataUrl
) {}
