package org.example.tas_backend.dtos;

import java.time.Instant;

public record ChatConversationDTO(
        String partnerId,
        String partnerName,
        String partnerEmail,
        String lastMessage,
        Instant timestamp
) {}
