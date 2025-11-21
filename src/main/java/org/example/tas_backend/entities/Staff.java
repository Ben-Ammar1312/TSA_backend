package org.example.tas_backend.entities;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.example.tas_backend.enums.Gender;
import org.hibernate.envers.Audited;

import java.time.LocalDate;

@Entity
@Getter
@Setter
@Audited(withModifiedFlag = true)
@Table(name = "staff",
        indexes = {@Index(name = "ix_staff_keycloak", columnList = "keycloakSub", unique = true)})
public class Staff {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(nullable = false, unique = true)
    String keycloakSub;          // Keycloak UUID

    String firstName;
    String lastName;

    @Enumerated(EnumType.STRING)
    Gender gender;

    LocalDate dateOfBirth;

    String nationalID;
    String email;
    String phoneNumber;

    String jobTitle;
    String department;

    String nationality;
    String residence;
    String visaStatus;

    @Embedded
    Address address;             // optional, if you later reuse Address

    @Embedded
    private Audit audit = new Audit();

    @PrePersist
    void prePersist() {
        if (audit == null) audit = new Audit();
    }
}