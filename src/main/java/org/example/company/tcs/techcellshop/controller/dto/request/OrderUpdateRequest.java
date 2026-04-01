package org.example.company.tcs.techcellshop.controller.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;
import org.example.company.tcs.techcellshop.util.OrderStatus;
import org.example.company.tcs.techcellshop.util.PaymentStatus;

@Data
public class OrderUpdateRequest {

    @Schema(example = "1", description = "Quantity that's being ordered")
    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantityOrder;

    @Schema(example = "SHIPPED", description = "Status of the order")
    @NotBlank(message = "Order status is required")
    @Pattern(
            regexp = "^(CREATED|PROCESSING|SHIPPED|DELIVERED|CANCELLED)$",
            message = "Invalid order status"
    )
    private OrderStatus statusOrder;

    @Schema(example = "2024-12-31", description = "Expected delivery date in yyyy-MM-dd format")
    @NotBlank(message = "Delivery date is required")
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "Delivery date must be yyyy-MM-dd")
    private String deliveryDate;

    @Schema(example = "CONFIRMWS", description = "Payment status of the order")
    @NotBlank(message = "Payment status is required")
    @Pattern(
            regexp = "^(PENDING|AUTHORIZED|PAID|FAILED|REFUNDED)$",
            message = "Invalid payment status"
    )
    private PaymentStatus paymentStatus;
}
