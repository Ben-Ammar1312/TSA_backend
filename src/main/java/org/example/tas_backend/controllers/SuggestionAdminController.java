package org.example.tas_backend.controllers;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.tas_backend.dtos.SuggestionDecisionDTO;
import org.example.tas_backend.entities.MappingSuggestion;
import org.example.tas_backend.enums.SuggestionStatus;
import org.example.tas_backend.repos.MappingSuggestionRepo;
import org.example.tas_backend.services.AiService;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/suggestions")
@RequiredArgsConstructor
public class SuggestionAdminController {
    private final MappingSuggestionRepo repo;
    private final AiService django; // to create alias in Django

    @GetMapping
    public List<MappingSuggestion> list(@RequestParam(required=false) String status) {
        var all = repo.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
        if (status == null) return all;
        return all.stream().filter(s -> s.getStatus().name().equals(status)).toList();
    }

    @PostMapping("/{id}/decide")
    @Transactional
    public MappingSuggestion decide(@PathVariable UUID id,
                                    @RequestBody SuggestionDecisionDTO body,
                                    Authentication auth) {
        var s = repo.findById(id).orElseThrow();
        if (!s.getStatus().equals(SuggestionStatus.PENDING)) return s;

        if ("accept".equalsIgnoreCase(body.getAction())) {
            // create alias in Django
            var alias = new org.example.tas_backend.dtos.SubjectAliasDTO(
                    null, s.getProposedTargetCode(), s.getSrcLabel(), null, s.getLanguage());
            django.createAlias(alias);

            s.setStatus(SuggestionStatus.ACCEPTED);
        } else {
            s.setStatus(SuggestionStatus.REJECTED);
        }
        s.setDecidedBy(auth != null ? auth.getName() : "admin");
        s.setDecidedAt(OffsetDateTime.now());
        s.setReason(body.getComment());
        return repo.save(s);
    }
}