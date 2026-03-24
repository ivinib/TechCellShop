package org.example.company.tcs.techcellshop.service;

import org.example.company.tcs.techcellshop.domain.Order;
import org.springframework.http.ResponseEntity;

import java.util.List;

public interface OrderService {
    Order saveOrder(Order order);
    Order getOrderById(Long id);
    List<Order> getAllOrders();
    Order updateOrder(Long id, Order order);
    void deleteOrder(Long id);
}
