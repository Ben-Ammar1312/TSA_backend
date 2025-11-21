package org.example.tas_backend.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.example.tas_backend.enums.Gender;

public record StaffRegistrationRequestDTO(

        @NotBlank
        @Size(min = 3, max = 50)
        String username,

        @NotBlank
        @Email
        String email,

        @NotBlank
        @Size(max = 80)
        String firstName,

        @NotBlank
        @Size(max = 80)
        String lastName,

        @NotBlank
        @Size(min = 8, max = 128)
        String password,

        @Size(max = 80)
        String jobTitle,

        @Size(max = 80)
        String department,

        @Size(max = 40)
        String phoneNumber,

        @Size(max = 40)
        String nationalID,
        @Size(max = 40)
        String dateOfBirth,

        Gender gender
) {}