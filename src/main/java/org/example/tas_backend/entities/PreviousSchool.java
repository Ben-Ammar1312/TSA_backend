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
public class PreviousSchool {
    @Id
    @GeneratedValue(strategy=IDENTITY)
    Long id;
    @ManyToOne(fetch=LAZY)
    Application application;

    String schoolName;
    String schoolCountry;
    String program;
    Float gpa;
    Integer graduationYear;

    @Embedded
    private Audit audit = new Audit();

    @PrePersist
    void prePersist() {
        if (audit == null) audit = new Audit();
    }
}