package org.example.company.tcs.techcellshop.service.impl;

import org.example.company.tcs.techcellshop.domain.Coupon;
import org.example.company.tcs.techcellshop.util.DiscountType;
import org.springframework.transaction.annotation.Transactional;
import org.example.company.tcs.techcellshop.controller.dto.coupon.CouponValidationResponseDto;
import org.example.company.tcs.techcellshop.exception.CouponValidationException;
import org.example.company.tcs.techcellshop.repository.CouponRepository;
import org.example.company.tcs.techcellshop.service.CouponService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Service
public class CouponServiceImpl  implements CouponService {

    private final CouponRepository couponRepository;

    public CouponServiceImpl(CouponRepository couponRepository) {
        this.couponRepository = couponRepository;
    }

    @Override
    public CouponValidationResponseDto validateCoupon(String code, BigDecimal orderAmount) {
        CouponValidationResponseDto responseDto = new CouponValidationResponseDto();

        try {
            BigDecimal discount = calculateDiscount(code, orderAmount);
            responseDto.setValid(true);
            responseDto.setDiscountAmount(discount);
            responseDto.setMessage("Coupon is valid");
        }catch (CouponValidationException ex) {
            responseDto.setValid(false);
            responseDto.setDiscountAmount(BigDecimal.ZERO);
            responseDto.setMessage(ex.getLocalizedMessage());
        }
        return responseDto;
    }


    @Override
    @Transactional(readOnly = true)
    public BigDecimal calculateDiscount(String code, BigDecimal orderAmount) {
        Coupon coupon = couponRepository.findByCodeIgnoreCase(code)
                .orElseThrow(() -> new CouponValidationException("Invalid coupon code: " + code));

        validateCouponRules(coupon, orderAmount);

        if (DiscountType.PERCENT.equals(coupon.getType()) ){
            return orderAmount
                    .multiply(coupon.getValue())
                    .divide(BigDecimal.valueOf(100));
        }

        BigDecimal fixed = coupon.getValue();

        return fixed.min(orderAmount);
    }

    @Transactional
    @Override
    public void registerCouponUsage(String code) {
        if (!couponRepository.existsByCodeIgnoreCase(code)) {
            throw new CouponValidationException("Invalid coupon code: " + code);
        }

        int updatedRows = couponRepository.incrementUsageIfAvailable(code);
        if (updatedRows == 0) {
            throw new CouponValidationException("Coupon usage limit reached for code: " + code);
        }
    }

    private void validateCouponRules(Coupon coupon, BigDecimal orderAmount){
        if (!Boolean.TRUE.equals(coupon.getActive())){
            throw new CouponValidationException("Coupon is not active: " + coupon.getCode());
        }

        OffsetDateTime now = OffsetDateTime.now();
        if (null != coupon.getStartsAt() && now.isBefore(coupon.getStartsAt())){
            throw new CouponValidationException("Coupon is not valid yet:" + coupon.getCode());
        } else if (null != coupon.getEndsAt() && now.isAfter(coupon.getEndsAt())) {
            throw new CouponValidationException("Coupon has expired: " + coupon.getCode());
        }

        if (null != coupon.getMinOrderAmount() && orderAmount.compareTo(coupon.getMinOrderAmount()) < 0) {
            throw new CouponValidationException("Order amount does not meet the minimum required for this coupon: " + coupon.getCode());
        }

        Integer quantityUsed = (null == coupon.getUsedCount()) ? 0 : coupon.getUsedCount();
        Integer usageLimit = coupon.getMaxUses();

        if (null != usageLimit && quantityUsed >= usageLimit) {
            throw new CouponValidationException("Coupon usage limit reached");
        }
    }
}
