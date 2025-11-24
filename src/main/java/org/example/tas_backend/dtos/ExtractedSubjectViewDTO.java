package org.example.tas_backend.dtos;

import org.example.tas_backend.enums.Scale;

import java.util.List;

public record ExtractedSubjectViewDTO(
        Long id,
        String rawName,
        Float rawScore,
        Scale rawScale,
        Integer year,
        Float sourceCoefficient,
        List<SubjectMappingViewDTO> mappings
) {}
