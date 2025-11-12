package org.example.tas_backend.repos;

import org.example.tas_backend.entities.MappingSuggestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MappingSuggestionRepo extends JpaRepository<MappingSuggestion, UUID> {
    Optional<MappingSuggestion> findByNormLabelAndProposedTargetCodeAndLanguage(String n, String t, String l);
}