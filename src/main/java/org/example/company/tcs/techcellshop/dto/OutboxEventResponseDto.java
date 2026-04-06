package org.example.company.tcs.techcellshop.dto;

import org.example.company.tcs.techcellshop.util.OutboxEventStatus;

import java.time.Instant;

public record OutboxEventResponseDto (
        Long id,
        String eventId,
        String eventType,
        String aggregateType,
        Long aggregateId,
        OutboxEventStatus status,
        int attempts,
        Instant createdAt,
        Instant nextAttemptAt,
        Instant sentAt,
        String lastError
){}
