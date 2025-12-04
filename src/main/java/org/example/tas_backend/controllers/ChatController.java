package org.example.tas_backend.controllers;

import lombok.RequiredArgsConstructor;
import org.example.tas_backend.dtos.ChatMessage;
import org.example.tas_backend.entities.ChatMessageEntity;
import org.example.tas_backend.repos.ChatMessageRepo;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Controller;

import java.time.Instant;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class ChatController {

    private final SimpMessagingTemplate broker;
    private final ChatMessageRepo repo;

    @MessageMapping("/chat")
    public void relay(@Payload ChatMessage incoming, JwtAuthenticationToken auth) {
        if (incoming == null || incoming.to() == null) return;
        String from = auth != null && auth.getName() != null ? auth.getName() : incoming.from();
        ChatMessage msg = new ChatMessage(
                incoming.id() != null ? incoming.id() : UUID.randomUUID().toString(),
                from,
                incoming.to(),
                incoming.body(),
                incoming.timestamp() != null ? incoming.timestamp() : Instant.now()
        );

        ChatMessageEntity entity = new ChatMessageEntity();
        entity.setId(msg.id());
        entity.setSender(msg.from());
        entity.setRecipient(msg.to());
        entity.setBody(msg.body());
        entity.setCreatedAt(msg.timestamp());
        repo.save(entity);

        // Deliver only to recipient; sender will update UI locally
        broker.convertAndSend("/topic/chat/" + msg.to(), msg);
    }
}
