package org.example.company.tcs.techcellshop.service.impl;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.example.company.tcs.techcellshop.domain.Coupon;
import org.example.company.tcs.techcellshop.util.DiscountType;
import org.example.company.tcs.techcellshop.util.MoneyUtils;
import org.springframework.transaction.annotation.Transactional;
import org.example.company.tcs.techcellshop.dto.coupon.CouponValidationResponseDto;
import org.example.company.tcs.techcellshop.exception.CouponValidationException;
import org.example.company.tcs.techcellshop.repository.CouponRepository;
import org.example.company.tcs.techcellshop.service.CouponService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.function.Supplier;

@Service
public class CouponServiceImpl  implements CouponService {

    private final CouponRepository couponRepository;

    private final Counter couponValidationCounter;
    private final Counter couponValidationFailureCounter;
    private final Counter couponUsageConflictCounter;
    private final Timer couponDiscountTimer;

    public CouponServiceImpl(CouponRepository couponRepository, MeterRegistry meterRegistry) {
        this.couponRepository = couponRepository;

        this.couponValidationCounter = meterRegistry.counter("techcellshop.coupons.validation.total");
        this.couponValidationFailureCounter = meterRegistry.counter("techcellshop.coupons.validation.failure");
        this.couponUsageConflictCounter = meterRegistry.counter("techcellshop.coupons.usage.conflict");
        this.couponDiscountTimer = meterRegistry.timer("techcellshop.coupons.discount.duration");
    }

    @Override
    public CouponValidationResponseDto validateCoupon(String code, BigDecimal orderAmount) {
        couponValidationCounter.increment();
        CouponValidationResponseDto responseDto = new CouponValidationResponseDto();

        try {
            BigDecimal discount = calculateDiscount(code, orderAmount);
            responseDto.setValid(true);
            responseDto.setDiscountAmount(discount);
            responseDto.setMessage("Coupon is valid");

        }catch (CouponValidationException ex) {
            couponValidationFailureCounter.increment();

            responseDto.setValid(false);
            responseDto.setDiscountAmount(MoneyUtils.zero());
            responseDto.setMessage(ex.getLocalizedMessage());
        }
        return responseDto;
    }


    @Override
    public BigDecimal calculateDiscount(String code, BigDecimal orderAmount) {
        Supplier<BigDecimal> discountCalculation = () -> {
            Coupon coupon = couponRepository.findByCodeIgnoreCase(code)
                    .orElseThrow(() -> new CouponValidationException("Invalid coupon code: " + code));

            BigDecimal normalizedOrderAmount = MoneyUtils.normalize(orderAmount);
            validateCouponRules(coupon, normalizedOrderAmount);

            if (DiscountType.PERCENT.equals(coupon.getType())) {
                BigDecimal percentDiscount = normalizedOrderAmount
                        .multiply(coupon.getValue())
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                return MoneyUtils.normalize(percentDiscount);
            }

            BigDecimal fixed = MoneyUtils.normalize(coupon.getValue());
            return MoneyUtils.normalize(fixed.min(normalizedOrderAmount));
        };

        return couponDiscountTimer.record(discountCalculation);
    }

    @Transactional
    @Override
    public void registerCouponUsage(String code) {
        if (!couponRepository.existsByCodeIgnoreCase(code)) {
            couponValidationFailureCounter.increment();
            throw new CouponValidationException("Invalid coupon code: " + code);
        }

        int updatedRows = couponRepository.incrementUsageIfAvailable(code);
        if (updatedRows == 0) {
            couponUsageConflictCounter.increment();
            throw new CouponValidationException("Coupon usage limit reached for code: " + code);
        }
    }

    private void validateCouponRules(Coupon coupon, BigDecimal orderAmount) {
        if (!Boolean.TRUE.equals(coupon.getActive())) {
            throw new CouponValidationException("Coupon is not active: " + coupon.getCode());
        }

        OffsetDateTime now = OffsetDateTime.now();
        if (null != coupon.getStartsAt() && now.isBefore(coupon.getStartsAt())) {
            throw new CouponValidationException("Coupon is not valid yet:" + coupon.getCode());
        } else if (null != coupon.getEndsAt() && now.isAfter(coupon.getEndsAt())) {
            throw new CouponValidationException("Coupon has expired: " + coupon.getCode());
        }

        if (null != coupon.getMinOrderAmount() && orderAmount.compareTo(coupon.getMinOrderAmount()) < 0) {
            throw new CouponValidationException("Order amount does not meet the minimum required for this coupon: " + coupon.getCode());
        }

        int quantityUsed = coupon.getUsedCount() == null ? 0 : coupon.getUsedCount();
        Integer usageLimit = coupon.getMaxUses();

        if (usageLimit != null && quantityUsed >= usageLimit) {
            throw new CouponValidationException("Coupon usage limit reached");
        }
    }
}
