package org.example.tas_backend.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.Audited;

import static jakarta.persistence.GenerationType.IDENTITY;
import jakarta.persistence.Column;

@Entity
@Getter
@Setter
@Audited(withModifiedFlag = true)
public class TargetSubject {
    @Id
    @GeneratedValue(strategy=IDENTITY)
    Long id;
    @Column(nullable=false, unique=true)
    String code;
    String name;
    Float coefficient;
}
