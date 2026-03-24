package org.example.company.tcs.techcellshop.controller.dto.response;

public record OrderResponse (
        Long idOrder,
        UserSummaryResponse user,
        DeviceSummaryResponse device,
        Integer quantityOrder,
        Double totalPriceOrder,
        String statusOrder,
        String orderDate,
        String deliveryDate,
        String paymentMethod,
        String paymentStatus
){}