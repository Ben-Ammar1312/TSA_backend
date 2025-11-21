package org.example.tas_backend.dtos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

public record ApplicationSubmitDTO(

        @Valid
        StudentApplicantProfileDTO profile,   // extended version with nationality, etc.

        @NotBlank
        String preferredProgram,

        @NotBlank
        String intakePeriod,

        String languageLevel
) {}