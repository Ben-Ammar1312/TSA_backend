package org.example.tas_backend.controllers;

import lombok.RequiredArgsConstructor;
import org.example.tas_backend.dtos.InviteDTO;
import org.example.tas_backend.dtos.InviteUpdateDTO;
import org.example.tas_backend.entities.Invite;
import org.example.tas_backend.enums.InviteStatus;
import org.example.tas_backend.repos.InviteRepo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@RestController
@RequestMapping("/admin/meet")
@RequiredArgsConstructor
public class InviteController {

    private final InviteRepo repo;
    private final SimpMessagingTemplate broker;

    @GetMapping("/invites")
    public List<Invite> list(@RequestParam(required = false) String targetUserId,
                             @RequestParam(required = false) String status) {
        if (targetUserId != null) {
            return repo.findByTargetUserId(targetUserId);
        }
        if (status != null) {
            try {
                return repo.findByStatus(InviteStatus.valueOf(status.toUpperCase()));
            } catch (IllegalArgumentException ignore) {}
        }
        return repo.findAll();
    }

    @PostMapping("/invites")
    public Invite create(@RequestBody InviteDTO dto,
                         @AuthenticationPrincipal Jwt jwt) {
        Invite inv = new Invite();
        inv.setTargetUserId(dto.getTargetUserId());
        inv.setTargetRole(dto.getTargetRole());
        inv.setTargetName(dto.getTargetName());
        inv.setScheduledTime(dto.getScheduledTime());
        inv.setNote(dto.getNote());
        inv.setStatus(InviteStatus.PENDING);
        inv.setCreatedBy(jwt != null ? jwt.getSubject() : "admin");
        inv = repo.save(inv);
        publish(inv);
        return inv;
    }

    @PostMapping("/invites/{id}/accept")
    public Invite accept(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        Invite inv = repo.findById(id).orElseThrow();
        enforceAccept(inv, jwt);
        inv.setStatus(InviteStatus.ACCEPTED);
        if (inv.getProposedTime() != null) {
            inv.setScheduledTime(inv.getProposedTime());
            inv.setProposedTime(null);
        }
        inv = repo.save(inv);
        publish(inv);
        return inv;
    }

    @PostMapping("/invites/{id}/propose")
    public Invite propose(@PathVariable UUID id, @RequestBody InviteUpdateDTO body, @AuthenticationPrincipal Jwt jwt) {
        Invite inv = repo.findById(id).orElseThrow();
        enforceTarget(inv, jwt);
        inv.setStatus(InviteStatus.PROPOSED);
        inv.setProposedTime(body.getProposedTime());
        inv.setScheduledTime(inv.getScheduledTime()); // keep existing
        inv = repo.save(inv);
        publish(inv);
        return inv;
    }

    @PostMapping("/invites/{id}/chat")
    public Invite chat(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        Invite inv = repo.findById(id).orElseThrow();
        enforceTarget(inv, jwt);
        inv.setStatus(InviteStatus.CHAT);
        inv = repo.save(inv);
        publish(inv);
        return inv;
    }

    @DeleteMapping("/invites/{id}")
    public void cancel(@PathVariable UUID id) {
        Invite inv = repo.findById(id).orElseThrow(() -> new NoSuchElementException("invite not found"));
        inv.setStatus(InviteStatus.CANCELLED);
        repo.save(inv);
        publish(inv);
    }

    @PostMapping("/invites/{id}/decline_proposed")
    public Invite declineProposed(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        Invite inv = repo.findById(id).orElseThrow();
        enforceCreator(inv, jwt);
        inv.setProposedTime(null);
        inv.setStatus(InviteStatus.PENDING);
        inv = repo.save(inv);
        publish(inv);
        return inv;
    }

    private void publish(Invite inv) {
        broker.convertAndSend("/topic/invites/" + inv.getTargetUserId(), inv);
        broker.convertAndSend("/topic/invites/admin", inv);
    }

    private void enforceTarget(Invite inv, Jwt jwt) {
        if (jwt == null || jwt.getSubject() == null || !jwt.getSubject().equals(inv.getTargetUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the target user can update this invite");
        }
    }

    private void enforceCreator(Invite inv, Jwt jwt) {
        if (jwt == null || jwt.getSubject() == null || inv.getCreatedBy() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the creator can perform this action");
        }
        if (!jwt.getSubject().equals(inv.getCreatedBy())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the creator can perform this action");
        }
    }

    private void enforceAccept(Invite inv, Jwt jwt) {
        if (jwt == null || jwt.getSubject() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden");
        }
        String sub = jwt.getSubject();
        boolean isTarget = sub.equals(inv.getTargetUserId());
        boolean isCreator = inv.getCreatedBy() != null && inv.getCreatedBy().equals(sub);
        if (!isTarget && !(isCreator && inv.getStatus() == InviteStatus.PROPOSED)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed to accept invite");
        }
    }
}
