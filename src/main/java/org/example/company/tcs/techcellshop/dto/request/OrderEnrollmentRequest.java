package org.example.company.tcs.techcellshop.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class OrderEnrollmentRequest {

    @Schema(example = "1", description = "User id who is placing the order")
    @NotNull(message = "User id is required")
    private Long idUser;

    @Schema(example = "1", description = "Device id being ordered")
    @NotNull(message = "Device id is required")
    private Long idDevice;

    @Schema(example = "1", description = "Quantity that's being ordered")
    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantityOrder;

    @Schema(example = "1", description = "Payment method used for the order")
    @NotBlank(message = "Payment method is required")
    @Pattern(
            regexp = "^(PIX|CREDIT_CARD|DEBIT_CARD|BOLETO)$",
            message = "Invalid payment method"
    )
    private String paymentMethod;
}