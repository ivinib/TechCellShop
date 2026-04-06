package org.example.company.tcs.techcellshop.dto.payment;

import lombok.Data;
import org.example.company.tcs.techcellshop.util.PaymentStatus;

import java.time.OffsetDateTime;

@Data
public class PaymentResponseDto {

    private Long orderId;

    private PaymentStatus paymentStatus;

    private OffsetDateTime processedAt;
}
