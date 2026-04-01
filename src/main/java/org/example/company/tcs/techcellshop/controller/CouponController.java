package org.example.company.tcs.techcellshop.controller;

import jakarta.validation.Valid;
import org.example.company.tcs.techcellshop.controller.dto.coupon.CouponValidationRequestDto;
import org.example.company.tcs.techcellshop.controller.dto.coupon.CouponValidationResponseDto;
import org.example.company.tcs.techcellshop.service.CouponService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/coupons")
public class CouponController {

    private final CouponService couponService;

    public CouponController(CouponService couponService) {
        this.couponService = couponService;
    }

    @PostMapping("/validate")
    public ResponseEntity<CouponValidationResponseDto> validate(
            @Valid @RequestBody CouponValidationRequestDto request) {
        return ResponseEntity.ok(couponService.validateCoupon(request.getCode(), request.getOrderAmount()));
    }
}
