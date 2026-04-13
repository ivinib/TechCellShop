package org.example.company.tcs.techcellshop.service.impl;

import org.example.company.tcs.techcellshop.domain.Order;
import org.example.company.tcs.techcellshop.dto.request.OrderEnrollmentRequest;
import org.example.company.tcs.techcellshop.dto.request.OrderUpdateRequest;
import org.example.company.tcs.techcellshop.exception.ResourceNotFoundException;
import org.example.company.tcs.techcellshop.mapper.RequestMapper;
import org.example.company.tcs.techcellshop.repository.OrderRepository;
import org.example.company.tcs.techcellshop.service.OrderService;
import org.example.company.tcs.techcellshop.service.impl.order.ApplyCouponToOrder;
import org.example.company.tcs.techcellshop.service.impl.order.CancelOrder;
import org.example.company.tcs.techcellshop.service.impl.order.PlaceOrder;
import org.example.company.tcs.techcellshop.service.impl.order.UpdateOrderStatus;
import org.example.company.tcs.techcellshop.util.OrderStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import static org.example.company.tcs.techcellshop.util.AppConstants.ORDER_NOT_FOUND;

@Service
public class OrderServiceImpl implements OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderServiceImpl.class);

    private final OrderRepository orderRepository;
    private final RequestMapper requestMapper;
    private final PlaceOrder placeOrder;
    private final UpdateOrderStatus updateOrderStatus;
    private final CancelOrder cancelOrder;
    private final ApplyCouponToOrder applyCouponToOrder;

    OrderServiceImpl(
            OrderRepository orderRepository,
            RequestMapper requestMapper,
            PlaceOrder placeOrder,
            UpdateOrderStatus updateOrderStatus,
            CancelOrder cancelOrder,
            ApplyCouponToOrder applyCouponToOrderUseCase
    ) {
        this.orderRepository = orderRepository;
        this.requestMapper = requestMapper;
        this.placeOrder = placeOrder;
        this.updateOrderStatus = updateOrderStatus;
        this.cancelOrder = cancelOrder;
        this.applyCouponToOrder = applyCouponToOrderUseCase;
    }

    @Override
    public Order saveOrder(Order order) {
        Order savedOrder = orderRepository.save(order);
        log.info("Order saved successfully");
        return savedOrder;
    }

    @Override
    public Order getOrderById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> {
                    log.info(ORDER_NOT_FOUND + id);
                    return new ResourceNotFoundException(ORDER_NOT_FOUND + id);
                });
    }

    @Override
    public Page<Order> getAllOrders(Pageable pageable) {
        Page<Order> orders = orderRepository.findAll(pageable);
        log.info("Returning orders page {} with {} element(s)", orders.getNumber(), orders.getNumberOfElements());
        return orders;
    }

    @Override
    public Page<Order> getOrdersForUser(String emailUser, Pageable pageable) {
        Page<Order> orders = orderRepository.findByUser_EmailUserIgnoreCase(emailUser, pageable);
        log.info(
                "Returning orders page {} with {} element(s) for user {}",
                orders.getNumber(),
                orders.getNumberOfElements(),
                emailUser
        );
        return orders;
    }

    @Override
    public Order updateOrder(Long id, OrderUpdateRequest request) {
        Order existingOrder = orderRepository.findById(id)
                .orElseThrow(() -> {
                    log.info(ORDER_NOT_FOUND + id);
                    return new ResourceNotFoundException(ORDER_NOT_FOUND + id);
                });

        requestMapper.updateOrder(existingOrder, request);
        Order updatedOrder = orderRepository.save(existingOrder);
        log.info("Order with id {} updated successfully", id);
        return updatedOrder;
    }

    @Override
    public void deleteOrder(Long id) {
        Order existingOrder = orderRepository.findById(id)
                .orElseThrow(() -> {
                    log.info(ORDER_NOT_FOUND + id);
                    return new ResourceNotFoundException(ORDER_NOT_FOUND + id);
                });

        orderRepository.delete(existingOrder);
        log.info("Order with id {} deleted successfully", id);
    }

    @Override
    public Order placeOrder(OrderEnrollmentRequest request, String authenticatedEmail) {
        return placeOrder.placeOrder(request, authenticatedEmail);
    }

    @Override
    public Order placeOrder(
            OrderEnrollmentRequest request,
            String authenticatedEmail,
            String idempotencyKey
    ) {
        return placeOrder.placeOrder(request, authenticatedEmail, idempotencyKey);
    }

    @Override
    public Order updateStatus(Long orderId, OrderStatus newStatus, String reason) {
        return updateOrderStatus.updateStatus(orderId, newStatus, reason);
    }

    @Override
    public Order cancelOrder(Long orderId, String reason) {
        return cancelOrder.cancelOrder(orderId, reason);
    }

    @Override
    public Order applyCoupon(Long orderId, String couponCode) {
        return applyCouponToOrder.applyCoupon(orderId, couponCode);
    }
}