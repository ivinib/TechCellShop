package org.example.company.tcs.techcellshop.service;

import org.example.company.tcs.techcellshop.controller.dto.coupon.CouponValidationResponseDto;

import java.math.BigDecimal;

public interface CouponService {

    CouponValidationResponseDto validateCoupon(String code, BigDecimal orderAmount);
    BigDecimal calculateDiscount(String code, BigDecimal orderAmount);
}
