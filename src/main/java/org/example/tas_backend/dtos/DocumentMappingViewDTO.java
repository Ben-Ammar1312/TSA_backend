package org.example.tas_backend.dtos;

import java.util.List;

public record DocumentMappingViewDTO(
        Long documentId,
        String filename,
        String rawText,
        List<ExtractedSubjectViewDTO> subjects
) {}
