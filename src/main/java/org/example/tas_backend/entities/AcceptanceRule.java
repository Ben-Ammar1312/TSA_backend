package org.example.tas_backend.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class AcceptanceRule {
    @Id
    private Long id = 1L;

    private int thresholdCount = 12;
    private int targetCount = 24;
}
