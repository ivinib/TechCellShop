package org.example.company.tcs.techcellshop.dto.payment;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PaymentActionRequestDto {

    @NotBlank
    private String transactionId;

    @NotNull
    @DecimalMin(value = "0.01")
    @Digits(integer = 10, fraction = 2, message = "Amount must have up to 10 integer digits and 2 decimal places")
    private BigDecimal amount;

    private String reason;
}
