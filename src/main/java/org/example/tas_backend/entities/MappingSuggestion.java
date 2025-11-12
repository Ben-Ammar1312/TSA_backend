package org.example.tas_backend.entities;

import jakarta.persistence.*;
import lombok.Data;
import org.example.tas_backend.enums.SuggestionStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name="mapping_suggestion",
        uniqueConstraints=@UniqueConstraint(columnNames={"normLabel","proposedTargetCode","language"}))
@Data
public class MappingSuggestion {
    @Id
    @GeneratedValue(strategy=GenerationType.UUID)
    private UUID id;

    @Column(nullable=false) private String srcLabel;          // raw user label
    @Column(nullable=false) private String normLabel;         // normalized
    @Column(nullable=false) private String proposedTargetCode;
    @Column(nullable=false, length=8) private String language; // "fr" or "en"
    @Column(nullable=false) private double score;             // LLM or fuzzy score
    @Column(nullable=false) private String method;            // llm_fallback, token_fuzzy_maybe
    @Column(nullable=false) @Enumerated(EnumType.STRING)
    private SuggestionStatus status = SuggestionStatus.PENDING;

    private String reason;        // optional model rationale
    private String createdBy;     // “django-llm” or user id
    private OffsetDateTime createdAt = OffsetDateTime.now();

    private String decidedBy;     // admin id
    private OffsetDateTime decidedAt;
}