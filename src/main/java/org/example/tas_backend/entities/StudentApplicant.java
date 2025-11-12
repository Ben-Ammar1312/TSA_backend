package org.example.tas_backend.entities;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.example.tas_backend.enums.Gender;
import org.hibernate.envers.Audited;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@Audited(withModifiedFlag = true)
@Table(name = "student_applicant",
        indexes = {@Index(name = "ix_student_keycloak",columnList = "keycloakSub",unique = true)})
public class StudentApplicant {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    @Column(nullable = false,unique = true)
    String keycloakSub;
    String firstName;
    String lastName;
    LocalDate dateOfBirth;
    @Enumerated(EnumType.STRING)
    Gender gender;
    String nationalID;
    String email;
    String phoneNumber;
    @Embedded
    Address address;
    @OneToMany(mappedBy = "student",cascade =CascadeType.ALL,orphanRemoval = true)
    List<Application> applications = new ArrayList<>();
    @Embedded
    private Audit audit = new Audit();

    @PrePersist
    void prePersist() {
        if (audit == null) audit = new Audit();
    }
}
