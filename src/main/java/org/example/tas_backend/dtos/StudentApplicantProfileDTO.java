package org.example.tas_backend.dtos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;

public record StudentApplicantProfileDTO(
        Long id,
        String keycloakSub,
        String firstName,
        String lastName,
        @Email String email,
        @Pattern(regexp = "^[+0-9 ()-]{6,}$", message = "invalid phone") String phoneNumber,
        String gender,                  // enum name
        @Past LocalDate dateOfBirth,
        String nationalID,
        @Valid AddressDTO address
) {}
