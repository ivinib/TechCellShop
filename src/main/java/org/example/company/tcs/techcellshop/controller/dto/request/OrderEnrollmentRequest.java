package org.example.company.tcs.techcellshop.controller.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
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

    @NotBlank(message = "Payment method is required")
    @Pattern(
            regexp = "^(PIX|CREDIT_CARD|DEBIT_CARD|BOLETO)$",
            message = "Invalid payment method"
    )
    private String paymentMethod;
}