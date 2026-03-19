package org.example.company.tcs.techcellshop.service;

import org.example.company.tcs.techcellshop.domain.Order;
import org.example.company.tcs.techcellshop.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<Order> saveOrder(Order order) {
        try{
            Order savedOrder = orderRepository.save(order);
            log.info("Order saved successfully");
            return ResponseEntity.ok(savedOrder);
        } catch (Exception e) {
            log.error("An error occurred while trying to save the order. Error:" + e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    public ResponseEntity<Order> getOrderById(Long id) {
        try {
            Order order = orderRepository.findById(id).orElse(null);
            if (order == null) {
                log.info("No order found with id {}", id);
                return ResponseEntity.notFound().build();
            }
            log.info("Returning order with id {}", id);
            return ResponseEntity.ok(order);
        } catch (Exception e) {
            log.error("An error occurred while trying to get the order by id. Error:" + e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    public ResponseEntity<List<Order>> getAllOrders() {
        try{
            List<Order> orders = orderRepository.findAll();
            if (orders.isEmpty()) {
                return ResponseEntity.noContent().build();
            }
            log.info("Returning all orders. Total of orders found: {}", orders.size());
            return ResponseEntity.ok(orders);
        } catch (Exception e) {
            log.error("An error occurred while trying to get all orders. Error:" + e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    public ResponseEntity<Order> updateOrder(Long id, Order order) {
        try{
            Order existingOrder = orderRepository.findById(id).orElse(null);
            if (existingOrder == null) {
                log.info("No order found with id {}", id);
                return ResponseEntity.notFound().build();
            }
            existingOrder.setUser(order.getUser());
            existingOrder.setDevice(order.getDevice());
            existingOrder.setQuantityOrder(order.getQuantityOrder());
            existingOrder.setTotalPriceOrder(order.getTotalPriceOrder());
            Order updatedOrder = orderRepository.save(existingOrder);
            log.info("Order with id {} updated successfully", id);
            return ResponseEntity.ok(updatedOrder);
        } catch (Exception e) {
            log.error("An error occurred while trying to update the order. Error:" + e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    public ResponseEntity<Void> deleteOrder(Long id) {
        try {
            Order existingOrder = orderRepository.findById(id).orElse(null);
            if (existingOrder == null) {
                log.info("No order found with id {}", id);
                return ResponseEntity.notFound().build();
            }
            orderRepository.delete(existingOrder);
            log.info("Order with id {} deleted successfully", id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("An error occurred while trying to delete the order. Error:" + e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
    }
}
