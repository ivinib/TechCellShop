package org.example.company.tcs.techcellshop.service;

import org.example.company.tcs.techcellshop.domain.Coupon;
import org.example.company.tcs.techcellshop.dto.coupon.CouponValidationResponseDto;
import org.example.company.tcs.techcellshop.exception.CouponValidationException;
import org.example.company.tcs.techcellshop.repository.CouponRepository;
import org.example.company.tcs.techcellshop.service.impl.CouponServiceImpl;
import org.example.company.tcs.techcellshop.util.DiscountType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CouponServiceImplTest {

    @Mock
    private CouponRepository couponRepository;

    @Mock
    private MeterRegistry meterRegistry;

    @Mock
    private Counter counter;

    @Mock
    private Timer timer;

    @InjectMocks
    private CouponServiceImpl couponService;

    @BeforeEach
    void setUp() {
        when(meterRegistry.counter(anyString())).thenReturn(counter);
        when(meterRegistry.timer(anyString())).thenReturn(timer);
        doAnswer(invocation -> invocation.<Supplier<BigDecimal>>getArgument(0).get())
                .when(timer).record(org.mockito.ArgumentMatchers.<Supplier<BigDecimal>>any());
    }

    @Test
    void registerCouponUsage_whenCouponDoesNotExist_shouldThrowInvalidCouponException() {
        when(couponRepository.existsByCodeIgnoreCase("OFF10")).thenReturn(false);

        assertThatThrownBy(() -> couponService.registerCouponUsage("OFF10"))
                .isInstanceOf(CouponValidationException.class)
                .hasMessage("Invalid coupon code: OFF10");
    }

    @Test
    void registerCouponUsage_whenUsageAvailable_shouldIncrementSuccessfully() {
        when(couponRepository.existsByCodeIgnoreCase("OFF10")).thenReturn(true);
        when(couponRepository.incrementUsageIfAvailable("OFF10")).thenReturn(1);

        assertThatCode(() -> couponService.registerCouponUsage("OFF10"))
                .doesNotThrowAnyException();

        verify(couponRepository).incrementUsageIfAvailable("OFF10");
    }

    @Test
    void registerCouponUsage_whenUsageLimitReached_shouldThrowException() {
        when(couponRepository.existsByCodeIgnoreCase("OFF10")).thenReturn(true);
        when(couponRepository.incrementUsageIfAvailable("OFF10")).thenReturn(0);

        assertThatThrownBy(() -> couponService.registerCouponUsage("OFF10"))
                .isInstanceOf(CouponValidationException.class)
                .hasMessage("Coupon usage limit reached for code: OFF10");
    }
    @Test
    void calculateDiscount_whenPercentCoupon_shouldReturnRoundedValue() {
        Coupon coupon = new Coupon();
        coupon.setCode("OFF10");
        coupon.setType(DiscountType.PERCENT);
        coupon.setValue(new BigDecimal("10.00"));
        coupon.setActive(true);
        coupon.setUsedCount(0);

        when(couponRepository.findByCodeIgnoreCase("OFF10")).thenReturn(Optional.of(coupon));

        BigDecimal result = couponService.calculateDiscount("OFF10", new BigDecimal("3999.90"));

        assertThat(result).isEqualByComparingTo("399.99");
    }

    @Test
    void calculateDiscount_whenFixedCouponExceedsOrderAmount_shouldCapAtOrderAmount() {
        Coupon coupon = new Coupon();
        coupon.setCode("OFF5000");
        coupon.setType(DiscountType.FIXED);
        coupon.setValue(new BigDecimal("5000.00"));
        coupon.setActive(true);
        coupon.setUsedCount(0);

        when(couponRepository.findByCodeIgnoreCase("OFF5000")).thenReturn(Optional.of(coupon));

        BigDecimal result = couponService.calculateDiscount("OFF5000", new BigDecimal("3999.90"));

        assertThat(result).isEqualByComparingTo("3999.90");
    }

    @Test
    void validateCoupon_whenInvalid_shouldReturnZeroDiscount() {
        when(couponRepository.findByCodeIgnoreCase("INVALID")).thenReturn(Optional.empty());

        CouponValidationResponseDto response = couponService.validateCoupon("INVALID", new BigDecimal("3999.90"));

        assertThat(response.isValid()).isFalse();
        assertThat(response.getDiscountAmount()).isEqualByComparingTo("0.00");
    }
}