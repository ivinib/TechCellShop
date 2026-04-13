package org.example.company.tcs.techcellshop.service.impl.order;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.transaction.Transactional;
import org.example.company.tcs.techcellshop.domain.Order;
import org.example.company.tcs.techcellshop.exception.CouponValidationException;
import org.example.company.tcs.techcellshop.exception.ResourceNotFoundException;
import org.example.company.tcs.techcellshop.repository.OrderRepository;
import org.example.company.tcs.techcellshop.service.CouponService;
import org.example.company.tcs.techcellshop.util.MoneyUtils;
import org.example.company.tcs.techcellshop.util.OrderStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

import static org.example.company.tcs.techcellshop.util.AppConstants.ORDER_NOT_FOUND;

@Component
public class ApplyCouponToOrder {
    private static final Logger log = LoggerFactory.getLogger(ApplyCouponToOrder.class);

    private final OrderRepository orderRepository;
    private final CouponService couponService;
    private final Timer couponApplyTimer;

    public ApplyCouponToOrder(OrderRepository orderRepository, CouponService couponService, MeterRegistry meterRegistry) {
        this.orderRepository = orderRepository;
        this.couponService = couponService;
        this.couponApplyTimer = meterRegistry.timer("techcellshop.orders.coupon.apply.duration");
    }

    @Transactional
    public Order applyCoupon(Long orderId, String couponCode) {
        return couponApplyTimer.record(() -> {
            Order order = getOrderOrThrow(orderId);

            if (order.getStatus().equals(OrderStatus.SHIPPED)
                    || order.getStatus().equals(OrderStatus.DELIVERED)
                    || order.getStatus().equals(OrderStatus.CANCELED)) {
                throw new CouponValidationException(
                        "Coupon cannot be applied for orders that are in shipped, delivered or canceled status"
                );
            }

            if (order.getCouponCode() != null && !order.getCouponCode().isBlank()) {
                if (order.getCouponCode().equalsIgnoreCase(couponCode)) {
                    return order;
                }

                throw new CouponValidationException(
                        "An order can have only one coupon applied. Current applied coupon: "
                                + order.getCouponCode()
                );
            }

            BigDecimal orderAmount = MoneyUtils.normalize(order.getTotalPriceOrder());
            BigDecimal discount = MoneyUtils.normalize(
                    couponService.calculateDiscount(couponCode, orderAmount)
            );
            BigDecimal finalAmount = MoneyUtils.subtractFloorZero(orderAmount, discount);

            order.setCouponCode(couponCode);
            order.setDiscountAmount(discount);
            order.setFinalAmount(finalAmount);

            Order saved = orderRepository.save(order);
            couponService.registerCouponUsage(couponCode);
            return saved;
        });
    }

    private Order getOrderOrThrow(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> {
                    log.info(ORDER_NOT_FOUND + orderId);
                    return new ResourceNotFoundException(ORDER_NOT_FOUND + orderId);
                });
    }
}
