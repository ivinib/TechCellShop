package org.example.company.tcs.techcellshop.service;

import org.example.company.tcs.techcellshop.domain.Order;
import org.example.company.tcs.techcellshop.exception.ResourceNotFoundException;
import org.example.company.tcs.techcellshop.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OrderServiceImpl implements OrderService{

    private OrderRepository orderRepository;
    private static final Logger log = LoggerFactory.getLogger(OrderServiceImpl.class);

    OrderServiceImpl(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
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
                    log.info("No order found with id {}", id);
                    return new ResourceNotFoundException("Order not found with id: " + id);
                });
    }

    @Override
    public List<Order> getAllOrders() {
        List<Order> orders = orderRepository.findAll();
        log.info("Returning all orders. Total found: {}", orders.size());
        return orders;
    }

    @Override
    public Order updateOrder(Long id, Order order) {
        Order existingOrder = orderRepository.findById(id)
                .orElseThrow(() -> {
                    log.info("No order found with id {}", id);
                    return new ResourceNotFoundException("Order not found with id: " + id);
                });

        existingOrder.setUser(order.getUser());
        existingOrder.setDevice(order.getDevice());
        existingOrder.setQuantityOrder(order.getQuantityOrder());
        existingOrder.setTotalPriceOrder(order.getTotalPriceOrder());
        existingOrder.setStatusOrder(order.getStatusOrder());
        existingOrder.setOrderDate(order.getOrderDate());
        existingOrder.setDeliveryDate(order.getDeliveryDate());
        existingOrder.setPaymentMethod(order.getPaymentMethod());
        existingOrder.setPaymentStatus(order.getPaymentStatus());

        Order updatedOrder = orderRepository.save(existingOrder);
        log.info("Order with id {} updated successfully", id);
        return updatedOrder;
    }

    @Override
    public void deleteOrder(Long id) {
        Order existingOrder = orderRepository.findById(id)
                .orElseThrow(() -> {
                    log.info("No order found with id {}", id);
                    return new ResourceNotFoundException("Order not found with id: " + id);
                });

        orderRepository.delete(existingOrder);
        log.info("Order with id {} deleted successfully", id);
    }
}
