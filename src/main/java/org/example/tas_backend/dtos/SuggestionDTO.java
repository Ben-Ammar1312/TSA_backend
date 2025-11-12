package org.example.tas_backend.dtos;

import lombok.Data;

@Data
public class SuggestionDTO {
    private String src_label;
    private String norm_label;
    private String proposed_target_code;
    private String language;
    private double score;
    private String method;   // "llm_fallback" etc.
    private String reason;   // optional
}
