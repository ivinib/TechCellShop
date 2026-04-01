package org.example.company.tcs.techcellshop.service.impl;

import org.example.company.tcs.techcellshop.exception.InvalidOrderStatusTransitionException;
import org.example.company.tcs.techcellshop.service.OrderStatusTransitionValidator;
import org.example.company.tcs.techcellshop.util.OrderStatus;
import org.springframework.stereotype.Service;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

@Service
public class OrderStatusTransitionValidatorImpl implements OrderStatusTransitionValidator {

    private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED_TRANSITIONS = Map.of(
            OrderStatus.CREATED, EnumSet.of(OrderStatus.PAID, OrderStatus.CANCELED),
            OrderStatus.PAID, EnumSet.of(OrderStatus.SHIPPED, OrderStatus.CANCELED),
            OrderStatus.SHIPPED, EnumSet.of(OrderStatus.DELIVERED),
            OrderStatus.DELIVERED, EnumSet.noneOf(OrderStatus.class),
            OrderStatus.CANCELED, EnumSet.noneOf(OrderStatus.class)
    );

    @Override
    public void validateTransition(OrderStatus currentStatus, OrderStatus newStatus) {
        if (null == currentStatus || null == newStatus){
            throw new IllegalArgumentException("Transiction of order status cannot be null");
        }
        if (currentStatus.equals(newStatus)) {
            return;
        }
        Set<OrderStatus> allowed = ALLOWED_TRANSITIONS.getOrDefault(currentStatus, EnumSet.noneOf(OrderStatus.class));

        if (!allowed.contains(newStatus)){
            throw new InvalidOrderStatusTransitionException(String.format("Invalid transition from %s to %s", currentStatus, newStatus));
        }
    }
}
