package org.example.company.tcs.techcellshop.service;

import org.example.company.tcs.techcellshop.dto.request.OrderEnrollmentRequest;
import org.example.company.tcs.techcellshop.dto.request.OrderUpdateRequest;
import org.example.company.tcs.techcellshop.dto.response.OrderResponse;
import org.example.company.tcs.techcellshop.domain.Order;
import org.example.company.tcs.techcellshop.util.OrderStatus;

import java.util.List;

public interface OrderService {
    Order saveOrder(Order order);
    Order getOrderById(Long id);
    List<Order> getAllOrders();
    Order updateOrder(Long id, OrderUpdateRequest request);
    void deleteOrder(Long id);
    Order placeOrder(OrderEnrollmentRequest request);
    OrderResponse updateStatus(Long orderId, OrderStatus newStatus, String reason);
    OrderResponse cancelOrder(Long orderId, String reason);
    OrderResponse applyCoupon(Long orderId, String couponCode);
    Order placeOrder(OrderEnrollmentRequest request, String idempotencyKey);
}
