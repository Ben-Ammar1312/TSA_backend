package org.example.tas_backend.controllers;

import lombok.RequiredArgsConstructor;
import org.example.tas_backend.dtos.ChatConversationDTO;
import org.example.tas_backend.dtos.ChatMessage;
import org.example.tas_backend.dtos.ChatUserDTO;
import org.example.tas_backend.repos.ChatMessageRepo;
import org.example.tas_backend.repos.StudentApplicantRepo;
import org.example.tas_backend.repos.StaffRepo;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ChatRestController {

    private final ChatMessageRepo repo;
    private final StudentApplicantRepo studentRepo;
    private final StaffRepo staffRepo;

    @GetMapping
    public ResponseEntity<List<ChatMessage>> thread(@AuthenticationPrincipal Jwt jwt,
                                                    @RequestParam("with") String other) {
        String me = jwt.getSubject();
        var list = repo.findThread(me, other).stream()
                .sorted(Comparator.comparing(m -> m.getCreatedAt(), Comparator.nullsLast(Comparator.naturalOrder())))
                .map(m -> new ChatMessage(
                        m.getId(),
                        m.getSender(),
                        m.getRecipient(),
                        m.getBody(),
                        m.getCreatedAt()
                ))
                .collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    @GetMapping("/history")
    public ResponseEntity<List<ChatConversationDTO>> history(@AuthenticationPrincipal Jwt jwt) {
        String me = jwt.getSubject();
        var all = repo.findRecentForUser(me);
        Map<String, ChatConversationDTO> latest = new java.util.LinkedHashMap<>();

        all.forEach(m -> {
            String partner = me.equals(m.getSender()) ? m.getRecipient() : m.getSender();
            var existing = latest.get(partner);
            if (existing == null || (existing.timestamp() != null && m.getCreatedAt() != null && m.getCreatedAt().isAfter(existing.timestamp()))) {
                var display = resolveUser(partner);
                latest.put(partner, new ChatConversationDTO(
                        partner,
                        display.name(),
                        display.email(),
                        m.getBody(),
                        m.getCreatedAt()
                ));
            }
        });

        var list = latest.values().stream()
                .sorted(Comparator.comparing(ChatConversationDTO::timestamp, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/search")
    public ResponseEntity<List<ChatUserDTO>> search(@RequestParam("term") String term) {
        String t = term == null ? "" : term.trim();
        if (t.length() < 2) return ResponseEntity.ok(List.of());

        var students = studentRepo.searchByTerm(t).stream()
                .map(s -> new ChatUserDTO(
                        s.getKeycloakSub(),
                        (s.getFirstName() != null ? s.getFirstName() : "") + " " + (s.getLastName() != null ? s.getLastName() : ""),
                        s.getEmail()
                ));
        var staff = staffRepo.searchByTerm(t).stream()
                .map(s -> new ChatUserDTO(
                        s.getKeycloakSub(),
                        (s.getFirstName() != null ? s.getFirstName() : "") + " " + (s.getLastName() != null ? s.getLastName() : ""),
                        s.getEmail()
                ));

        var combined = java.util.stream.Stream.concat(students, staff)
                .filter(u -> u.id() != null && !u.id().isBlank())
                .collect(Collectors.toMap(ChatUserDTO::id, u -> u, (a, b) -> a))
                .values()
                .stream()
                .collect(Collectors.toList());

        return ResponseEntity.ok(combined);
    }

    private ChatUserDTO resolveUser(String sub) {
        var student = studentRepo.findByKeycloakSub(sub).orElse(null);
        if (student != null) {
            return new ChatUserDTO(
                    student.getKeycloakSub(),
                    ((student.getFirstName() != null ? student.getFirstName() : "") + " " + (student.getLastName() != null ? student.getLastName() : "")).trim(),
                    student.getEmail()
            );
        }
        var staff = staffRepo.findByKeycloakSub(sub).orElse(null);
        if (staff != null) {
            return new ChatUserDTO(
                    staff.getKeycloakSub(),
                    ((staff.getFirstName() != null ? staff.getFirstName() : "") + " " + (staff.getLastName() != null ? staff.getLastName() : "")).trim(),
                    staff.getEmail()
            );
        }
        return new ChatUserDTO(sub, sub, null);
    }
}
