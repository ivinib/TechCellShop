package org.example.company.tcs.techcellshop.controller.dto.response;

import org.example.company.tcs.techcellshop.util.OrderStatus;
import org.example.company.tcs.techcellshop.util.PaymentStatus;

public record OrderResponse (
        Long idOrder,
        UserSummaryResponse user,
        DeviceSummaryResponse device,
        Integer quantityOrder,
        Double totalPriceOrder,
        OrderStatus statusOrder,
        String orderDate,
        String deliveryDate,
        String paymentMethod,
        PaymentStatus paymentStatus,
        String couponCode,
        java.math.BigDecimal discountAmount,
        java.math.BigDecimal finalAmount,
        String canceledReason
){}