package org.example.tas_backend.dtos;

public record SubjectMappingViewDTO(
        Long id,
        String targetCode,
        String targetName,
        Float confidence,
        String method,
        Float normalizedScore
) {}
