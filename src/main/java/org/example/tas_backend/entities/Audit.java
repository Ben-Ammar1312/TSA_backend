package org.example.tas_backend.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Embeddable
@Getter
@Setter
public class Audit {
    @CreationTimestamp
    @Column(nullable=false, updatable=false)
    Instant createdAt;
    @UpdateTimestamp
    @Column(nullable=false)
    Instant updatedAt;
    String createdBy;
    String updatedBy;
}