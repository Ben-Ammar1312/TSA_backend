package org.example.tas_backend.dtos;

import java.util.List;

public record ApplicationMappingViewDTO(
        Long applicationId,
        String studentName,
        List<DocumentMappingViewDTO> documents
) {}
