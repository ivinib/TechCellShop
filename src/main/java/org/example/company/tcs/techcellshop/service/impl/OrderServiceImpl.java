package org.example.company.tcs.techcellshop.service.impl;

import jakarta.transaction.Transactional;
import org.example.company.tcs.techcellshop.controller.dto.response.OrderResponse;
import org.example.company.tcs.techcellshop.exception.CouponValidationException;
import org.example.company.tcs.techcellshop.exception.InvalidOrderStatusTransitionException;
import org.example.company.tcs.techcellshop.mapper.ResponseMapper;
import org.example.company.tcs.techcellshop.messaging.OrderCreatedDomainEvent;
import org.example.company.tcs.techcellshop.controller.dto.request.OrderEnrollmentRequest;
import org.example.company.tcs.techcellshop.controller.dto.request.OrderUpdateRequest;
import org.example.company.tcs.techcellshop.domain.Device;
import org.example.company.tcs.techcellshop.domain.Order;
import org.example.company.tcs.techcellshop.domain.User;
import org.example.company.tcs.techcellshop.exception.ResourceNotFoundException;
import org.example.company.tcs.techcellshop.mapper.RequestMapper;
import org.example.company.tcs.techcellshop.repository.DeviceRepository;
import org.example.company.tcs.techcellshop.repository.OrderRepository;
import org.example.company.tcs.techcellshop.repository.UserRepository;
import org.example.company.tcs.techcellshop.service.CouponService;
import org.example.company.tcs.techcellshop.service.DeviceService;
import org.example.company.tcs.techcellshop.service.OrderService;
import org.example.company.tcs.techcellshop.service.OrderStatusTransitionValidator;
import org.example.company.tcs.techcellshop.util.OrderStatus;
import org.example.company.tcs.techcellshop.util.PaymentStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private static final Logger log = LoggerFactory.getLogger(OrderServiceImpl.class);
    private final RequestMapper requestMapper;
    private final UserRepository userRepository;
    private final DeviceRepository deviceRepository;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final OrderStatusTransitionValidator orderStatusTransitionValidator;
    private final DeviceService deviceService;
    
    private static final String ORDER_NOT_FOUND = "No order found with id: ";
    private final ResponseMapper responseMapper;
    private final CouponService couponService;

    OrderServiceImpl(OrderRepository orderRepository, RequestMapper requestMapper, UserRepository userRepository, DeviceRepository deviceRepository, ApplicationEventPublisher applicationEventPublisher, DeviceService deviceService, OrderStatusTransitionValidator orderStatusTransitionValidator, ResponseMapper responseMapper, CouponService couponService) {
        this.orderRepository = orderRepository;
        this.requestMapper = requestMapper;
        this.userRepository = userRepository;
        this.deviceRepository = deviceRepository;
        this.applicationEventPublisher = applicationEventPublisher;
        this.deviceService = deviceService;
        this.orderStatusTransitionValidator = orderStatusTransitionValidator;
        this.responseMapper = responseMapper;
        this.couponService = couponService;
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
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + request.getIdUser()));

        Device device = deviceRepository.findById(request.getIdDevice())
                .orElseThrow(() -> new ResourceNotFoundException("Device not found with id: " + request.getIdDevice()));

        Integer quantity = request.getQuantityOrder();
        deviceService.reserveStock(device.getIdDevice(), quantity);

        Order order = new Order();
        order.setUser(user);
        order.setDevice(device);
        order.setQuantityOrder(quantity);
        order.setPaymentMethod(request.getPaymentMethod());

        order.setStatus(OrderStatus.CREATED);
        order.setPaymentStatus(PaymentStatus.PENDING);
        order.setOrderDate(LocalDate.now().toString());
        order.setDeliveryDate(LocalDate.now().plusDays(5).toString());

        double total = device.getDevicePrice() * quantity;
        order.setTotalPriceOrder(total);
        order.setDiscountAmount(BigDecimal.ZERO);
        order.setFinalAmount(BigDecimal.valueOf(total));

        Order saved = orderRepository.save(order);
        applicationEventPublisher.publishEvent(new OrderCreatedDomainEvent(saved));

        return saved;
    }

    private Order getOrderOrThrow(Long orderId){
        return orderRepository.findById(orderId)
                .orElseThrow(() -> {
                    log.info(ORDER_NOT_FOUND + orderId);
                    return new ResourceNotFoundException(ORDER_NOT_FOUND + orderId);
                });
    }

    @Transactional
    @Override
    public OrderResponse updateStatus(Long orderId, OrderStatus newStatus, String reason){
        if (newStatus.equals(OrderStatus.CANCELED)){
            return cancelOrder(orderId, reason);
        }

        Order order = getOrderOrThrow(orderId);
        orderStatusTransitionValidator.validateTransition(order.getStatus(), newStatus);

        if (newStatus.equals(OrderStatus.SHIPPED) && !order.getPaymentStatus().equals(PaymentStatus.CONFIRMED)){
            throw new InvalidOrderStatusTransitionException("Order cannot be shipped without confirmed payment");
        }

        order.setStatus(newStatus);
        Order orderUpdated = orderRepository.save(order);

        return responseMapper.toOrderResponse(orderUpdated);
    }

    @Transactional
    @Override
    public OrderResponse cancelOrder(Long orderId, String reason){
        Order order = getOrderOrThrow(orderId);
        if (order.getStatus().equals(OrderStatus.CANCELED)){
            return responseMapper.toOrderResponse(order);
        }

        if (order.getStatus().equals(OrderStatus.SHIPPED) || order.getStatus().equals(OrderStatus.DELIVERED)){
            throw new InvalidOrderStatusTransitionException("Cannot cancel an order that has already been shipped or delivered");
        }

        order.setStatus(OrderStatus.CANCELED);
        order.setCanceledReason((reason == null || reason.isBlank()) ? "Order canceled by the user" : reason);

        deviceService.releaseStock(order.getDevice().getIdDevice(), order.getQuantityOrder());
        Order updatedOrder = orderRepository.save(order);
        return responseMapper.toOrderResponse(updatedOrder);

    }

    @Transactional
    @Override
    public OrderResponse applyCoupon(Long orderId, String couponCode) {
        Order order = getOrderOrThrow(orderId);

        if (order.getStatus().equals(OrderStatus.SHIPPED) || order.getStatus().equals(OrderStatus.DELIVERED) || order.getStatus().equals(OrderStatus.CANCELED)){
            throw new CouponValidationException("Coupon cannot be applied for orders that are in shipped, delivered or canceled status");
        } else if (null != order.getCouponCode() && !order.getCouponCode().isBlank()) {
            if (order.getCouponCode().equalsIgnoreCase(couponCode)){
                return responseMapper.toOrderResponse(order);
            }
            throw new CouponValidationException("An order can have only one coupon applied. Current applied coupon: " + order.getCouponCode());
        }

        BigDecimal orderAmount = BigDecimal.valueOf(order.getTotalPriceOrder());
        BigDecimal discount = couponService.calculateDiscount(couponCode, orderAmount);
        BigDecimal finalAmount = orderAmount.subtract(discount);

        if (finalAmount.compareTo(BigDecimal.ZERO) < 0){
            finalAmount = BigDecimal.ZERO;
        }

        order.setCouponCode(couponCode);
        order.setDiscountAmount(discount);
        order.setFinalAmount(finalAmount);

        Order saved = orderRepository.save(order);
        couponService.registerCouponUsage(couponCode);

        responseMapper.toOrderResponse(saved);
    }
}
