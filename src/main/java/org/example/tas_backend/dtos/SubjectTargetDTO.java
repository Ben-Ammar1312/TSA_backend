package org.example.tas_backend.dtos;



import java.util.UUID;
import java.time.OffsetDateTime;

public record SubjectTargetDTO(
        UUID id,
        String code,
        String title_fr,
        String title_en,
        String categorie,
        Integer level,
        String norm_label,
        Boolean is_active,
        Integer version,
        OffsetDateTime created_at,
        OffsetDateTime updated_at
) {}