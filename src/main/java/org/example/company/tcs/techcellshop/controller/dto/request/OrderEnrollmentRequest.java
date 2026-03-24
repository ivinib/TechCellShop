package org.example.company.tcs.techcellshop.controller.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class OrderEnrollmentRequest {

    @NotNull(message = "User id is required")
    private Long idUser;

    @NotNull(message = "Device id is required")
    private Long idDevice;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantityOrder;

    @NotNull(message = "Total price is required")
    @DecimalMin(value = "0.01", message = "Total price must be greater than zero")
    private Double totalPriceOrder;

    @NotBlank(message = "Order status is required")
    @Pattern(
            regexp = "^(CREATED|PROCESSING|SHIPPED|DELIVERED|CANCELLED)$",
            message = "Invalid order status"
    )
    private String statusOrder;

    @NotBlank(message = "Order date is required")
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "Order date must be yyyy-MM-dd")
    private String orderDate;

    @NotBlank(message = "Delivery date is required")
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "Delivery date must be yyyy-MM-dd")
    private String deliveryDate;

    @NotBlank(message = "Payment method is required")
    @Pattern(
            regexp = "^(PIX|CREDIT_CARD|DEBIT_CARD|BOLETO)$",
            message = "Invalid payment method"
    )
    private String paymentMethod;

    @NotBlank(message = "Payment status is required")
    @Pattern(
            regexp = "^(PENDING|AUTHORIZED|PAID|FAILED|REFUNDED)$",
            message = "Invalid payment status"
    )
    private String paymentStatus;
}
