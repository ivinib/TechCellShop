package org.example.company.tcs.techcellshop.messaging;

import org.example.company.tcs.techcellshop.util.OrderStatus;
import org.example.company.tcs.techcellshop.util.PaymentStatus;

import java.time.Instant;

public record OrderCreatedEvent(
        String eventId,
        Long orderId,
        Long userId,
        Long deviceId,
        Integer quantity,
        Double totalPrice,
        String paymentMethod,
        OrderStatus status,
        PaymentStatus paymentStatus,
        Instant occurredAt
) {}