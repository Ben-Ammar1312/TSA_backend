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
        enforceTarget(inv, jwt);
        inv.setStatus(InviteStatus.ACCEPTED);
        inv.setProposedTime(null);
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

    private void publish(Invite inv) {
        broker.convertAndSend("/topic/invites/" + inv.getTargetUserId(), inv);
        broker.convertAndSend("/topic/invites/admin", inv);
    }

    private void enforceTarget(Invite inv, Jwt jwt) {
        if (jwt == null || jwt.getSubject() == null || !jwt.getSubject().equals(inv.getTargetUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the target user can update this invite");
        }
    }
}
