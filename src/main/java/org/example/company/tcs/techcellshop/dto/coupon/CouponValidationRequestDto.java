package org.example.company.tcs.techcellshop.dto.coupon;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CouponValidationRequestDto {

    @NotBlank
    private String code;

    @NotNull
    @DecimalMin(value = "0.01")
    @Digits(integer = 10, fraction = 2, message = "Order amount must have up to 10 integer digits and 2 decimal places")
    private BigDecimal orderAmount;
}
