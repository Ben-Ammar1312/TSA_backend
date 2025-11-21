package org.example.tas_backend.dtos;

public record StudentFileDTO(
        Long id,
        String originalFilename,
        String contentType,
        Long sizeBytes
        // you can add a download URL later if you want
) {}