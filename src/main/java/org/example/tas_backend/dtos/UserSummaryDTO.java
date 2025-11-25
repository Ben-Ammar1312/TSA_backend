package org.example.tas_backend.dtos;

import java.util.List;

public record UserSummaryDTO(
        String id,          // Keycloak UUID
        String username,
        String email,
        String firstName,
        String lastName,
        List<String> roles,
        String phoneNumber,
        String jobTitle
) {}
