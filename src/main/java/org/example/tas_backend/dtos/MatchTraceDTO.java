package org.example.tas_backend.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MatchTraceDTO(
        String src,
        String target,
        String method,
        Double score,
        @JsonProperty("target_title") String targetTitle,
        @JsonProperty("target_level") Integer targetLevel,
        @JsonProperty("target_coef") Integer targetCoef
) {}
