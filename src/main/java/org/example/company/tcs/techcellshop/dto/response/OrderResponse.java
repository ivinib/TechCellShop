package org.example.company.tcs.techcellshop.dto.response;

import org.example.company.tcs.techcellshop.util.OrderStatus;
import org.example.company.tcs.techcellshop.util.PaymentStatus;

import java.math.BigDecimal;

public record OrderResponse (
        Long idOrder,
        UserSummaryResponse user,
        DeviceSummaryResponse device,
        Integer quantityOrder,
        BigDecimal totalPriceOrder,
        OrderStatus statusOrder,
        String orderDate,
        String deliveryDate,
        String paymentMethod,
        PaymentStatus paymentStatus,
        String couponCode,
        BigDecimal discountAmount,
        BigDecimal finalAmount,
        String canceledReason
){}