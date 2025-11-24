package org.example.tas_backend.dtos;

public record ApplicationSummaryDTO(
        Long id,
        String studentName,
        String status,
        int documentsCount
) {}
