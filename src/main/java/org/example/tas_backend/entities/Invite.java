package org.example.tas_backend.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.example.tas_backend.enums.InviteStatus;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
public class Invite {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String targetUserId;

    @Column(nullable = false)
    private String targetRole; // student | staff

    private String targetName;

    @Column(nullable = false)
    private String scheduledTime; // ISO string from UI

    private String proposedTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InviteStatus status = InviteStatus.PENDING;

    private String note;

    @CreationTimestamp
    private OffsetDateTime createdAt;

    private String createdBy;
}
