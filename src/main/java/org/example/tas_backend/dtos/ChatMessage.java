package org.example.tas_backend.dtos;

import java.time.Instant;

public record ChatMessage(
        String id,
        String from,
        String to,
        String body,
        Instant timestamp
) {}
