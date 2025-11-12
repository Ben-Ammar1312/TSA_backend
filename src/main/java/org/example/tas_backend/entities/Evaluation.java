package org.example.tas_backend.entities;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.example.tas_backend.enums.EvaluationStatus;
import org.hibernate.envers.Audited;

import static jakarta.persistence.FetchType.LAZY;
import static jakarta.persistence.GenerationType.IDENTITY;

@Entity
@Getter
@Setter
@Audited(withModifiedFlag = true)
@Table(indexes = {@Index(name="ix_eval_app", columnList="application_id"),
        @Index(name="ix_eval_status", columnList="status")})
public class Evaluation {
    @Id @GeneratedValue(strategy=IDENTITY)
    Long id;
    @ManyToOne(fetch=LAZY)
    Application application;
    Float equivalenceScore;
    String aiComments;
    @Enumerated(EnumType.STRING)
    EvaluationStatus status;

    // roll-up numbers
    Float confidence;
    Float scoreMaxPossible;

    @Embedded
    private Audit audit = new Audit();

    @PrePersist
    void prePersist() {
        if (audit == null) audit = new Audit();
    }
}