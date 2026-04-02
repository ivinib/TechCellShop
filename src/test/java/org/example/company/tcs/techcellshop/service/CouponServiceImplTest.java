package org.example.company.tcs.techcellshop.service;

import org.example.company.tcs.techcellshop.exception.CouponValidationException;
import org.example.company.tcs.techcellshop.repository.CouponRepository;
import org.example.company.tcs.techcellshop.service.impl.CouponServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CouponServiceImplTest {

    @Mock
    private CouponRepository couponRepository;

    @InjectMocks
    private CouponServiceImpl couponService;

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
}