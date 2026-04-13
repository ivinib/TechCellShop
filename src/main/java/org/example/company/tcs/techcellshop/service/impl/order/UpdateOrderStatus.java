package org.example.company.tcs.techcellshop.service.impl.order;

import jakarta.transaction.Transactional;
import org.example.company.tcs.techcellshop.domain.Order;
import org.example.company.tcs.techcellshop.exception.InvalidOrderStatusTransitionException;
import org.example.company.tcs.techcellshop.exception.ResourceNotFoundException;
import org.example.company.tcs.techcellshop.repository.OrderRepository;
import org.example.company.tcs.techcellshop.service.OrderStatusTransitionValidator;
import org.example.company.tcs.techcellshop.util.OrderStatus;
import org.example.company.tcs.techcellshop.util.PaymentStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import static org.example.company.tcs.techcellshop.util.AppConstants.ORDER_NOT_FOUND;

@Component
public class UpdateOrderStatus {
    private static final Logger log = LoggerFactory.getLogger(UpdateOrderStatus.class);

    private final OrderRepository orderRepository;
    private final OrderStatusTransitionValidator orderStatusTransitionValidator;
    private final CancelOrder cancelOrder;

    public UpdateOrderStatus(
            OrderRepository orderRepository,
            OrderStatusTransitionValidator orderStatusTransitionValidator,
            CancelOrder cancelOrder
    ) {
        this.orderRepository = orderRepository;
        this.orderStatusTransitionValidator = orderStatusTransitionValidator;
        this.cancelOrder = cancelOrder;
    }

    @Transactional
    public Order updateStatus(Long orderId, OrderStatus newStatus, String reason) {
        if (newStatus.equals(OrderStatus.CANCELED)) {
            return cancelOrder.cancelOrder(orderId, reason);
        }

        Order order = getOrderOrThrow(orderId);
        orderStatusTransitionValidator.validateTransition(order.getStatus(), newStatus);

        if (newStatus.equals(OrderStatus.PAID)
                && !order.getPaymentStatus().equals(PaymentStatus.CONFIRMED)) {
            throw new InvalidOrderStatusTransitionException(
                    "Order cannot be marked as paid without confirmed payment"
            );
        }

        if (newStatus.equals(OrderStatus.SHIPPED)
                && !order.getPaymentStatus().equals(PaymentStatus.CONFIRMED)) {
            throw new InvalidOrderStatusTransitionException(
                    "Order cannot be shipped without confirmed payment"
            );
        }

        order.setStatus(newStatus);
        return orderRepository.save(order);
    }

    private Order getOrderOrThrow(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> {
                    log.info(ORDER_NOT_FOUND + orderId);
                    return new ResourceNotFoundException(ORDER_NOT_FOUND + orderId);
                });
    }
}
