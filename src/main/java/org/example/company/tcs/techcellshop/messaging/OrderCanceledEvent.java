package org.example.company.tcs.techcellshop.messaging;

import java.time.OffsetDateTime;

public record OrderCanceledEvent(
        Long orderId,
        String reason,
        OffsetDateTime occurredAt
) { }
