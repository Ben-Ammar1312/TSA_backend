package org.example.tas_backend.repos;

import org.example.tas_backend.entities.MappingSuggestion;
import org.example.tas_backend.enums.SuggestionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MappingSuggestionRepo extends JpaRepository<MappingSuggestion, UUID> {
    Optional<MappingSuggestion> findByNormLabelAndProposedTargetCodeAndLanguage(String n, String t, String l);

    List<MappingSuggestion> findBySrcLabelIgnoreCaseAndProposedTargetCodeAndStatus(String srcLabel,
                                                                                   String targetCode,
                                                                                   SuggestionStatus status);

    List<MappingSuggestion> findByStatus(SuggestionStatus status, Sort sort);
}
