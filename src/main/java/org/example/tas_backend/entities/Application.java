package org.example.tas_backend.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.example.tas_backend.enums.ApplicationStatus;
import org.hibernate.envers.Audited;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static jakarta.persistence.CascadeType.ALL;
import static jakarta.persistence.FetchType.LAZY;
import static jakarta.persistence.GenerationType.IDENTITY;

@Entity
@Getter
@Setter
@Audited(withModifiedFlag = true)
@Table(indexes = {@Index(name="ix_app_student", columnList="student_id"),
        @Index(name="ix_app_status", columnList="status")})
public class Application {
    @Id @GeneratedValue(strategy=IDENTITY) Long id;

    @ManyToOne(fetch=LAZY) @JoinColumn(nullable=false)
    StudentApplicant student;
    String preferredProgram;
    String intakePeriod;
    String languageLevel;

    @Enumerated(EnumType.STRING)
    ApplicationStatus status;
    String decisionBy;
    Instant decisionDate;

    // AI evaluation snapshot for the application
    @OneToMany(mappedBy="application", cascade=ALL, orphanRemoval=true)
    List<Evaluation> evaluations = new ArrayList<>();

    // Interview attempts
    @OneToMany(mappedBy="application", cascade=ALL, orphanRemoval=true)
    List<Interview> interviews = new ArrayList<>();

    // School history tied to this application
    @OneToMany(mappedBy="application", cascade=ALL, orphanRemoval=true)
    List<PreviousSchool> schools = new ArrayList<>();

    // Uploaded docs tied to this application
    @OneToMany(mappedBy="application", cascade=ALL, orphanRemoval=true)
    List<Document> documents = new ArrayList<>();

    @Embedded
    private Audit audit = new Audit();

    @PrePersist
    void prePersist() {
        if (audit == null) audit = new Audit();
    }
}
