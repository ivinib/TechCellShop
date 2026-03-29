package org.example.company.tcs.techcellshop.messaging;

import java.time.Instant;

public record OrderCreatedEvent(
        String eventId,
        Long orderId,
        Long userId,
        Long deviceId,
        Integer quantity,
        Double totalPrice,
        String paymentMethod,
        String status,
        String paymentStatus,
        Instant occurredAt
) {}