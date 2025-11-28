package org.example.tas_backend.controllers;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.tas_backend.dtos.MappingUpdateDTO;
import org.example.tas_backend.dtos.SubjectMappingViewDTO;
import org.example.tas_backend.entities.SubjectMapping;
import org.example.tas_backend.enums.SuggestionStatus;
import org.example.tas_backend.repos.MappingSuggestionRepo;
import org.example.tas_backend.repos.SubjectMappingRepo;
import org.example.tas_backend.repos.TargetSubjectRepo;
import org.example.tas_backend.services.AiService;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@RestController
@RequestMapping("/admin/mappings")
@RequiredArgsConstructor
@Slf4j
public class SubjectMappingAdminController {

    private final SubjectMappingRepo mappingRepo;
    private final TargetSubjectRepo targetRepo;
    private final MappingSuggestionRepo suggestionRepo;
    private final AiService ai;

    @PatchMapping("/{id}")
    @Transactional
    public SubjectMappingViewDTO updateMapping(@PathVariable Long id,
                                               @RequestBody MappingUpdateDTO body) {
        if (body == null || !StringUtils.hasText(body.getTargetCode())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "targetCode is required");
        }

        SubjectMapping mapping = mappingRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "mapping not found"));

        var newTarget = targetRepo.findByCode(body.getTargetCode())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "target code not found: " + body.getTargetCode()));

        var previousTarget = mapping.getTargetSubject();
        var previousMethod = mapping.getMethod();
        var extracted = mapping.getExtractedSubject();

        Optional<SubjectMapping> duplicate = mappingRepo.findByExtractedSubjectAndTargetSubject(extracted, newTarget);
        SubjectMapping toSave = mapping;

        if (duplicate.isPresent() && !duplicate.get().getId().equals(mapping.getId())) {
            // Avoid unique constraint clashes; reuse the existing mapping record for this target.
            mappingRepo.delete(mapping);
            toSave = duplicate.get();
        }

        toSave.setTargetSubject(newTarget);
        toSave.setAuto(false);
        if (body.getConfidence() != null) {
            toSave.setConfidence(body.getConfidence());
            toSave.setNormalizedScore(body.getConfidence());
        }
        toSave.setMethod("admin_override");

        var saved = mappingRepo.save(toSave);

        boolean targetChanged = previousTarget == null ||
                !previousTarget.getCode().equalsIgnoreCase(newTarget.getCode());

        if (targetChanged) {
            cleanupAfterChange(previousMethod,
                    previousTarget != null ? previousTarget.getCode() : null,
                    extracted != null ? extracted.getRawName() : null);
        }

        return new SubjectMappingViewDTO(
                saved.getId(),
                saved.getTargetSubject() != null ? saved.getTargetSubject().getCode() : null,
                saved.getTargetSubject() != null ? saved.getTargetSubject().getName() : null,
                saved.getConfidence(),
                saved.getMethod(),
                saved.getNormalizedScore()
        );
    }

    private void cleanupAfterChange(String previousMethod, String previousTargetCode, String rawLabel) {
        if (!StringUtils.hasText(previousMethod) || !StringUtils.hasText(rawLabel)) return;

        String lower = previousMethod.toLowerCase();

        if (lower.contains("fuzzy")) {
            dropAlias(rawLabel, previousTargetCode);
        }
        if (lower.contains("llm")) {
            dropSuggestion(rawLabel, previousTargetCode);
        }
    }

    private void dropAlias(String rawLabel, String targetCode) {
        if (!StringUtils.hasText(targetCode)) return;
        try {
            var aliases = ai.listAliases(null, targetCode, rawLabel);
            aliases.stream()
                    .filter(a -> a.label() != null && a.label().equalsIgnoreCase(rawLabel))
                    .forEach(a -> ai.deleteAlias(a.id().toString()));
        } catch (Exception ex) {
            log.warn("Failed to delete alias for {} -> {}: {}", rawLabel, targetCode, ex.getMessage());
        }
    }

    private void dropSuggestion(String rawLabel, String targetCode) {
        if (!StringUtils.hasText(targetCode)) return;
        var pending = suggestionRepo.findBySrcLabelIgnoreCaseAndProposedTargetCodeAndStatus(
                rawLabel, targetCode, SuggestionStatus.PENDING);
        pending.forEach(suggestionRepo::delete);
    }
}
