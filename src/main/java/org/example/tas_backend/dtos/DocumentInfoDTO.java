package org.example.tas_backend.dtos;

public record DocumentInfoDTO(
        String url,
        String filename,
        String mimeType,
        Long sizeBytes,
        Integer pages
) {}
