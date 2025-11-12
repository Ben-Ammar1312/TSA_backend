package org.example.tas_backend.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.example.tas_backend.enums.InterviewResult;
import org.hibernate.envers.Audited;

import java.time.LocalDateTime;

import static jakarta.persistence.FetchType.LAZY;
import static jakarta.persistence.GenerationType.IDENTITY;

@Entity
@Getter
@Setter
@Audited(withModifiedFlag = true)
@Table(indexes = {@Index(name="ix_int_app", columnList="application_id"),
        @Index(name="ix_int_date", columnList="interviewDate")})
public class Interview {
    @Id
    @GeneratedValue(strategy=IDENTITY)
    Long id;
    @ManyToOne(fetch=LAZY)
    Application application;

    LocalDateTime interviewDate;
    String interviewLink;
    @Enumerated(EnumType.STRING)
    InterviewResult result;
    String notes;
    String interviewerName;

    @Embedded
    private Audit audit = new Audit();

    @PrePersist
    void prePersist() {
        if (audit == null) audit = new Audit();
    }
}
