package org.example.tas_backend.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.example.tas_backend.enums.Scale;
import org.hibernate.envers.Audited;

import static jakarta.persistence.FetchType.LAZY;
import static jakarta.persistence.GenerationType.IDENTITY;

@Entity
@Getter
@Setter
@Audited(withModifiedFlag = true)
@Table(indexes = {@Index(name="ix_subj_doc", columnList="document_id")})
public class ExtractedSubject {
    @Id @GeneratedValue(strategy=IDENTITY)
    Long id;
    @ManyToOne(fetch=LAZY)
    Document document;

    String rawName;
    Float rawScore;
    @Enumerated(EnumType.STRING)
    Scale rawScale; // OUT_OF_20, OUT_OF_100
    Integer year;
    Float sourceCoefficient;
    @Embedded
    private Audit audit = new Audit();

    @PrePersist
    void prePersist() {
        if (audit == null) audit = new Audit();
    }
}