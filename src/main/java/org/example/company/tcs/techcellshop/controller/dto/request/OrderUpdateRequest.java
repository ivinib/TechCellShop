package org.example.company.tcs.techcellshop.controller.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class OrderUpdateRequest {

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantityOrder;

    @NotBlank(message = "Order status is required")
    @Pattern(
            regexp = "^(CREATED|PROCESSING|SHIPPED|DELIVERED|CANCELLED)$",
            message = "Invalid order status"
    )
    private String statusOrder;

    @NotBlank(message = "Delivery date is required")
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "Delivery date must be yyyy-MM-dd")
    private String deliveryDate;

    @NotBlank(message = "Payment status is required")
    @Pattern(
            regexp = "^(PENDING|AUTHORIZED|PAID|FAILED|REFUNDED)$",
            message = "Invalid payment status"
    )
    private String paymentStatus;
}
