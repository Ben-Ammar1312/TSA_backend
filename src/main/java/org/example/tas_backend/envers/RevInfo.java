package org.example.tas_backend.envers;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.*;

@Entity
@Table(name = "revinfo")
@RevisionEntity(UserRevisionListener.class)
@Getter @Setter
public class RevInfo implements java.io.Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // ok for Postgres
    @RevisionNumber
    private Integer id;

    @RevisionTimestamp
    private long timestamp;

    @Column(length = 64, nullable = false)
    private String actorId;


    @Column(length = 256, nullable = false)      // human-readable name
    private String actor;
}

