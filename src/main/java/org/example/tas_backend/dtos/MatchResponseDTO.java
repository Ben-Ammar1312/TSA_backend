package org.example.tas_backend.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record MatchResponseDTO(
        List<String> matched,
        @JsonProperty("coverage_pct") Double coveragePct,
        List<MatchTraceDTO> trace
) {}
