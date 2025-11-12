package org.example.tas_backend.dtos;



import java.util.UUID;

public record SubjectAliasDTO(
        UUID id,
        String target,     // Django uses target code via SlugRelatedField
        String label,
        String norm_label, // read-only from server
        String language    // "fr" | "en"
) {}