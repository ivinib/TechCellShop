package org.example.company.tcs.techcellshop.dto.coupon;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CouponValidationResponseDto {

    private boolean valid;

    private BigDecimal discountAmount;

    private String message;
}
