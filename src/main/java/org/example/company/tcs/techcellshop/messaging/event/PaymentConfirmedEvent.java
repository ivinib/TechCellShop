package org.example.company.tcs.techcellshop.messaging.event;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record PaymentConfirmedEvent(
        Long orderId,
        String transactionId,
        BigDecimal amount,
        OffsetDateTime occurredAt
) { }
