package org.example.tas_backend.controllers;

import lombok.RequiredArgsConstructor;
import org.example.tas_backend.dtos.SuggestionDTO;
import org.example.tas_backend.entities.MappingSuggestion;
import org.example.tas_backend.repos.MappingSuggestionRepo;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/suggestions")
@RequiredArgsConstructor
public class SuggestionController {
    private final MappingSuggestionRepo repo;

    @PostMapping
    public MappingSuggestion create(@RequestBody SuggestionDTO dto) {
        var existing = repo.findByNormLabelAndProposedTargetCodeAndLanguage(
                dto.getNorm_label(), dto.getProposed_target_code(), dto.getLanguage());
        if (existing.isPresent()) return existing.get(); // idempotent

        var s = new MappingSuggestion();
        s.setSrcLabel(dto.getSrc_label());
        s.setNormLabel(dto.getNorm_label());
        s.setProposedTargetCode(dto.getProposed_target_code());
        s.setLanguage(dto.getLanguage());
        s.setScore(dto.getScore());
        s.setMethod(dto.getMethod());
        s.setReason(dto.getReason());
        s.setCreatedBy("django-llm");
        return repo.save(s);
    }
}