package org.example.tas_backend.dtos;

import java.time.Instant;
import java.util.List;

public record RevisionSummaryDTO(String entity, Object entityId,Number revision, Instant at , String by, List<String> changedFields) {
}
