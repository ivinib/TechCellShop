package org.example.company.tcs.techcellshop.service;

import jakarta.transaction.Transactional;
import org.example.company.tcs.techcellshop.messaging.OrderCreatedDomainEvent;
import org.example.company.tcs.techcellshop.controller.dto.request.OrderEnrollmentRequest;
import org.example.company.tcs.techcellshop.controller.dto.request.OrderUpdateRequest;
import org.example.company.tcs.techcellshop.domain.Device;
import org.example.company.tcs.techcellshop.domain.Order;
import org.example.company.tcs.techcellshop.domain.User;
import org.example.company.tcs.techcellshop.exception.InsufficientStockException;
import org.example.company.tcs.techcellshop.exception.ResourceNotFoundException;
import org.example.company.tcs.techcellshop.mapper.RequestMapper;
import org.example.company.tcs.techcellshop.repository.DeviceRepository;
import org.example.company.tcs.techcellshop.repository.OrderRepository;
import org.example.company.tcs.techcellshop.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class OrderServiceImpl implements OrderService{

    private final OrderRepository orderRepository;
    private static final Logger log = LoggerFactory.getLogger(OrderServiceImpl.class);
    private final RequestMapper requestMapper;
    private final UserRepository userRepository;
    private final DeviceRepository deviceRepository;
    private final ApplicationEventPublisher applicationEventPublisher;
    
    private static final String ORDER_NOT_FOUND = "No order found with id: ";

    OrderServiceImpl(OrderRepository orderRepository, RequestMapper requestMapper, UserRepository userRepository, DeviceRepository deviceRepository, ApplicationEventPublisher applicationEventPublisher) {
        this.orderRepository = orderRepository;
        this.requestMapper = requestMapper;
        this.userRepository = userRepository;
        this.deviceRepository = deviceRepository;
        this.applicationEventPublisher = applicationEventPublisher;
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
    public List<Order> getAllOrders() {
        List<Order> orders = orderRepository.findAll();
        log.info("Returning all orders. Total found: {}", orders.size());
        return orders;
    }

    @Override
    public Order updateOrder(Long id, OrderUpdateRequest request) {
        Order existingOrder = orderRepository.findById(id)
                .orElseThrow(() -> {
                    log.info(ORDER_NOT_FOUND + id);
                    return new ResourceNotFoundException(ORDER_NOT_FOUND +  id);
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
    
    @Transactional
    @Override
    public Order placeOrder(OrderEnrollmentRequest request) {
        User user = userRepository.findById(request.getIdUser())
                .orElseThrow(() -> {
                    log.info("No user found with id {}", request.getIdUser());
                    return new ResourceNotFoundException("User not found with id: " + request.getIdUser());
                });
        Device device = deviceRepository.findById(request.getIdDevice())
                .orElseThrow(() -> {
                    log.info("No device found with id {}", request.getIdDevice());
                    return new ResourceNotFoundException("Device not found with id: " + request.getIdDevice());
                });
        
        Integer quantity = request.getQuantityOrder();
        if (device.getDeviceStock() < quantity) {
            log.info("The requested quantity of the product {} is unavailable. Stock available: {}", device.getNameDevice(), device.getDeviceStock());
            throw new InsufficientStockException("The requested quantity of the product "+ device.getNameDevice() +" is unavailable. Stock available: " + device.getDeviceStock());
        }
        device.setDeviceStock(device.getDeviceStock() - quantity);
        deviceRepository.save(device);

        Order order = new Order();
        order.setUser(user);
        order.setDevice(device);
        order.setQuantityOrder(quantity);
        order.setPaymentMethod(request.getPaymentMethod());

        // Server-controlled fields
        order.setStatusOrder("CREATED");
        order.setPaymentStatus("PENDING");
        order.setOrderDate(LocalDate.now().toString());
        order.setDeliveryDate(LocalDate.now().plusDays(5).toString());

        double total = device.getDevicePrice() * quantity;
        order.setTotalPriceOrder(total);

        Order saved = orderRepository.save(order);
        applicationEventPublisher.publishEvent(new OrderCreatedDomainEvent(saved));
        return saved;
    }
}
