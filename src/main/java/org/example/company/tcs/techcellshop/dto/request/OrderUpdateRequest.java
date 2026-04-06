package org.example.company.tcs.techcellshop.dto.request;

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
    @NotNull(message = "Order status is required")
    private OrderStatus statusOrder;

    @Schema(example = "2024-12-31", description = "Expected delivery date in yyyy-MM-dd format")
    @NotBlank(message = "Delivery date is required")
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "Delivery date must be yyyy-MM-dd")
    private String deliveryDate;

    @Schema(example = "CONFIRMED", description = "Payment status of the order")
    @NotNull(message = "Payment status is required")
    private PaymentStatus paymentStatus;
}
