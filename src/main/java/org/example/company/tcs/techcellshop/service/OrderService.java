package org.example.company.tcs.techcellshop.service;

import org.example.company.tcs.techcellshop.domain.Order;
import org.springframework.http.ResponseEntity;

import java.util.List;

public interface OrderService {
    ResponseEntity<Order> saveOrder(Order order);

    ResponseEntity<Order> getOrderById(Long id);

    ResponseEntity<List<Order>> getAllOrders();

    ResponseEntity<Order> updateOrder(Long id, Order order);

    ResponseEntity<Void> deleteOrder(Long id);
}
