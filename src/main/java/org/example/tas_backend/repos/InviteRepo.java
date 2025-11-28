package org.example.tas_backend.repos;

import org.example.tas_backend.entities.Invite;
import org.example.tas_backend.enums.InviteStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface InviteRepo extends JpaRepository<Invite, UUID> {
    List<Invite> findByTargetUserId(String targetUserId);
    List<Invite> findByStatus(InviteStatus status);
}
