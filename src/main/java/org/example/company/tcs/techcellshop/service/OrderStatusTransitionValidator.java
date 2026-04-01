package org.example.company.tcs.techcellshop.service;

import org.example.company.tcs.techcellshop.util.OrderStatus;

public interface OrderStatusTransitionValidator {
    void validateTransition(OrderStatus currentStatus, OrderStatus newStatus);
}
