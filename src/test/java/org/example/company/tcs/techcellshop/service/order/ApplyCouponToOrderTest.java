package org.example.company.tcs.techcellshop.service.order;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.example.company.tcs.techcellshop.domain.Order;
import org.example.company.tcs.techcellshop.exception.CouponValidationException;
import org.example.company.tcs.techcellshop.exception.ResourceNotFoundException;
import org.example.company.tcs.techcellshop.repository.OrderRepository;
import org.example.company.tcs.techcellshop.service.CouponService;
import org.example.company.tcs.techcellshop.service.impl.order.ApplyCouponToOrder;
import org.example.company.tcs.techcellshop.util.OrderStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ApplyCouponToOrderTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CouponService couponService;

    @Mock
    private MeterRegistry meterRegistry;

    @Mock
    private Timer timer;

    private ApplyCouponToOrder applyCouponToOrder;

    private Order order;

    @BeforeEach
    void setUp() {
        when(meterRegistry.timer(anyString())).thenReturn(timer);
        doAnswer(invocation -> {
            Supplier<?> supplier = invocation.getArgument(0);
            return supplier.get();
        }).when(timer).record(org.mockito.ArgumentMatchers.<Supplier<?>>any());

        applyCouponToOrder = new ApplyCouponToOrder(orderRepository, couponService, meterRegistry);

        order = new Order();
        order.setIdOrder(1L);
        order.setStatus(OrderStatus.CREATED);
        order.setTotalPriceOrder(new BigDecimal("3999.90"));
        order.setDiscountAmount(new BigDecimal("0.00"));
        order.setFinalAmount(new BigDecimal("3999.90"));
    }

    @Nested
    @DisplayName("applyCoupon")
    class ApplyCoupon {

        @Test
        @DisplayName("should apply coupon successfully")
        void shouldApplyCouponSuccessfully() {
            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
            when(couponService.calculateDiscount("WELCOME10", new BigDecimal("3999.90")))
                    .thenReturn(new BigDecimal("10.00"));
            when(orderRepository.save(order)).thenReturn(order);

            Order result = applyCouponToOrder.applyCoupon(1L, "WELCOME10");

            assertThat(result.getCouponCode()).isEqualTo("WELCOME10");
            assertThat(result.getDiscountAmount()).isEqualByComparingTo("10.00");
            assertThat(result.getFinalAmount()).isEqualByComparingTo("3989.90");

            verify(couponService).registerCouponUsage("WELCOME10");
            verify(orderRepository).save(order);
        }

        @Test
        @DisplayName("should return same order when same coupon already applied")
        void shouldReturnSameOrderWhenSameCouponAlreadyApplied() {
            order.setCouponCode("WELCOME10");
            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

            Order result = applyCouponToOrder.applyCoupon(1L, "WELCOME10");

            assertThat(result).isSameAs(order);

            verify(orderRepository, never()).save(order);
            verify(couponService, never()).registerCouponUsage(anyString());
        }

        @Test
        @DisplayName("should throw when a different coupon is already applied")
        void shouldThrowWhenDifferentCouponAlreadyApplied() {
            order.setCouponCode("OLD10");
            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

            assertThatThrownBy(() -> applyCouponToOrder.applyCoupon(1L, "WELCOME10"))
                    .isInstanceOf(CouponValidationException.class)
                    .hasMessage("An order can have only one coupon applied. Current applied coupon: OLD10");
        }

        @Test
        @DisplayName("should throw when order is shipped")
        void shouldThrowWhenOrderIsShipped() {
            order.setStatus(OrderStatus.SHIPPED);
            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

            assertThatThrownBy(() -> applyCouponToOrder.applyCoupon(1L, "WELCOME10"))
                    .isInstanceOf(CouponValidationException.class)
                    .hasMessage("Coupon cannot be applied for orders that are in shipped, delivered or canceled status");
        }

        @Test
        @DisplayName("should throw when order is delivered")
        void shouldThrowWhenOrderIsDelivered() {
            order.setStatus(OrderStatus.DELIVERED);
            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

            assertThatThrownBy(() -> applyCouponToOrder.applyCoupon(1L, "WELCOME10"))
                    .isInstanceOf(CouponValidationException.class)
                    .hasMessage("Coupon cannot be applied for orders that are in shipped, delivered or canceled status");
        }

        @Test
        @DisplayName("should throw when order is canceled")
        void shouldThrowWhenOrderIsCanceled() {
            order.setStatus(OrderStatus.CANCELED);
            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

            assertThatThrownBy(() -> applyCouponToOrder.applyCoupon(1L, "WELCOME10"))
                    .isInstanceOf(CouponValidationException.class)
                    .hasMessage("Coupon cannot be applied for orders that are in shipped, delivered or canceled status");
        }

        @Test
        @DisplayName("should throw when order does not exist")
        void shouldThrowWhenOrderDoesNotExist() {
            when(orderRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> applyCouponToOrder.applyCoupon(99L, "WELCOME10"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("No order found with id: 99");
        }
    }

}
