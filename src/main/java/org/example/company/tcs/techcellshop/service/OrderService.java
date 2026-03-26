package org.example.company.tcs.techcellshop.service;

import org.example.company.tcs.techcellshop.controller.dto.request.OrderEnrollmentRequest;
import org.example.company.tcs.techcellshop.controller.dto.request.OrderUpdateRequest;
import org.example.company.tcs.techcellshop.domain.Order;

import java.util.List;

public interface OrderService {
    Order saveOrder(Order order);
    Order getOrderById(Long id);
    List<Order> getAllOrders();
    Order updateOrder(Long id, OrderUpdateRequest request);
    void deleteOrder(Long id);
    Order placeOrder(OrderEnrollmentRequest request);
}
