package org.example.tas_backend.dtos;

import lombok.Data;

@Data
public class SuggestionDecisionDTO {
    private String action;
    private String comment;
}