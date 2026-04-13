package org.example.company.tcs.techcellshop.service.impl.order;

import jakarta.transaction.Transactional;
import org.example.company.tcs.techcellshop.domain.Order;
import org.example.company.tcs.techcellshop.exception.InvalidOrderStatusTransitionException;
import org.example.company.tcs.techcellshop.exception.ResourceNotFoundException;
import org.example.company.tcs.techcellshop.repository.OrderRepository;
import org.example.company.tcs.techcellshop.service.DeviceService;
import org.example.company.tcs.techcellshop.util.OrderStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import static org.example.company.tcs.techcellshop.util.AppConstants.ORDER_NOT_FOUND;

@Component
public class CancelOrder {
    private static final Logger log = LoggerFactory.getLogger(CancelOrder.class);

    private final OrderRepository orderRepository;
    private final DeviceService deviceService;

    public CancelOrder(OrderRepository orderRepository, DeviceService deviceService) {
        this.orderRepository = orderRepository;
        this.deviceService = deviceService;
    }

    @Transactional
    public Order cancelOrder(Long orderId, String reason) {
        Order order = getOrderOrThrow(orderId);

        if (order.getStatus().equals(OrderStatus.CANCELED)) {
            return order;
        }

        if (order.getStatus().equals(OrderStatus.SHIPPED)
                || order.getStatus().equals(OrderStatus.DELIVERED)) {
            throw new InvalidOrderStatusTransitionException(
                    "Cannot cancel an order that has already been shipped or delivered"
            );
        }

        order.setStatus(OrderStatus.CANCELED);
        order.setCanceledReason(
                reason == null || reason.isBlank()
                        ? "Order canceled by the user"
                        : reason
        );

        deviceService.releaseStock(order.getDevice().getIdDevice(), order.getQuantityOrder());
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
