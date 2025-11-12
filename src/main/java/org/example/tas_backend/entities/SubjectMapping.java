package org.example.tas_backend.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.Audited;

import static jakarta.persistence.FetchType.LAZY;
import static jakarta.persistence.GenerationType.IDENTITY;

@Entity
@Getter
@Setter
@Audited(withModifiedFlag = true)
@Table(uniqueConstraints = @UniqueConstraint(name="uk_map_subject_target", columnNames={"extractedSubject_id","targetSubject_id"}))
public class SubjectMapping {
    @Id
    @GeneratedValue(strategy=IDENTITY)
    Long id;
    @ManyToOne(fetch=LAZY)
    ExtractedSubject extractedSubject;
    @ManyToOne(fetch=LAZY)
    TargetSubject targetSubject;

    Float confidence;
    Boolean auto; // auto vs admin override
    Float normalizedScore; // after scale + coefficient

    @Embedded
    private Audit audit = new Audit();

    @PrePersist
    void prePersist() {
        if (audit == null) audit = new Audit();
    }
}