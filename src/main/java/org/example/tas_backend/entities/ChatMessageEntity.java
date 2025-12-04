package org.example.tas_backend.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Getter
@Setter
@Table(name = "chat_message", indexes = {
        @Index(name = "ix_chat_sender_recipient", columnList = "sender,recipient")
})
public class ChatMessageEntity {
    @Id
    private String id;
    private String sender;
    private String recipient;
    private String body;
    private Instant createdAt;
}
