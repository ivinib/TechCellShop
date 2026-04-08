package org.example.company.tcs.techcellshop.service;

import org.example.company.tcs.techcellshop.dto.request.OrderEnrollmentRequest;
import org.example.company.tcs.techcellshop.dto.request.OrderUpdateRequest;
import org.example.company.tcs.techcellshop.domain.Order;
import org.example.company.tcs.techcellshop.util.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


public interface OrderService {
    Order saveOrder(Order order);
    Order getOrderById(Long id);
    Page<Order> getAllOrders(Pageable pageable);
    Order updateOrder(Long id, OrderUpdateRequest request);
    void deleteOrder(Long id);
    Order placeOrder(OrderEnrollmentRequest request);
    Order updateStatus(Long orderId, OrderStatus newStatus, String reason);
    Order cancelOrder(Long orderId, String reason);
    Order applyCoupon(Long orderId, String couponCode);
    Order placeOrder(OrderEnrollmentRequest request, String idempotencyKey);
}
