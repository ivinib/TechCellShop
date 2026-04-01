package org.example.company.tcs.techcellshop.messaging.event;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record OrderCreatedEvent(
        Long orderId,
        Long userId,
        BigDecimal totalAmount,
        OffsetDateTime occurredAt
) { }
