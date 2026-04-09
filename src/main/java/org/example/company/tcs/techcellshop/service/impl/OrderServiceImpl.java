package org.example.company.tcs.techcellshop.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import org.example.company.tcs.techcellshop.domain.*;
import org.example.company.tcs.techcellshop.exception.CouponValidationException;
import org.example.company.tcs.techcellshop.exception.InvalidOrderStatusTransitionException;
import org.example.company.tcs.techcellshop.dto.request.OrderEnrollmentRequest;
import org.example.company.tcs.techcellshop.dto.request.OrderUpdateRequest;
import org.example.company.tcs.techcellshop.exception.ResourceNotFoundException;
import org.example.company.tcs.techcellshop.mapper.RequestMapper;
import org.example.company.tcs.techcellshop.messaging.OrderCreatedEvent;
import org.example.company.tcs.techcellshop.repository.*;
import org.example.company.tcs.techcellshop.service.CouponService;
import org.example.company.tcs.techcellshop.service.DeviceService;
import org.example.company.tcs.techcellshop.service.OrderService;
import org.example.company.tcs.techcellshop.service.OrderStatusTransitionValidator;
import org.example.company.tcs.techcellshop.util.MoneyUtils;
import org.example.company.tcs.techcellshop.util.OrderStatus;
import org.example.company.tcs.techcellshop.util.OutboxEventStatus;
import org.example.company.tcs.techcellshop.util.PaymentStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private static final Logger log = LoggerFactory.getLogger(OrderServiceImpl.class);
    private final RequestMapper requestMapper;
    private final UserRepository userRepository;
    private final DeviceRepository deviceRepository;
    private final OrderStatusTransitionValidator orderStatusTransitionValidator;
    private final DeviceService deviceService;
    private final OrderIdempondencyRepository orderIdempondencyRepository;
    private final OutboxEventRepository outboxEventRepository;
    
    private static final String ORDER_NOT_FOUND = "No order found with id: ";
    private final CouponService couponService;
    private final ObjectMapper objectMapper;

    private final Counter orderPlacedCounter;
    private final Counter orderPlaceConflictCounter;
    private final Counter orderIdempotencyHitCounter;
    private final Counter orderIdempotencyConflictCounter;
    private final Timer orderPlaceTimer;
    private final Timer couponApplyTimer;

    OrderServiceImpl(OrderRepository orderRepository, RequestMapper requestMapper, UserRepository userRepository, DeviceRepository deviceRepository, DeviceService deviceService, OrderStatusTransitionValidator orderStatusTransitionValidator, CouponService couponService, OrderIdempondencyRepository orderIdempondencyRepository, ObjectMapper objectMapper, OutboxEventRepository outboxEventRepository, MeterRegistry meterRegistry) {
        this.orderRepository = orderRepository;
        this.requestMapper = requestMapper;
        this.userRepository = userRepository;
        this.deviceRepository = deviceRepository;
        this.deviceService = deviceService;
        this.orderStatusTransitionValidator = orderStatusTransitionValidator;
        this.couponService = couponService;
        this.orderIdempondencyRepository = orderIdempondencyRepository;
        this.objectMapper = objectMapper;
        this.outboxEventRepository = outboxEventRepository;

        this.orderPlacedCounter = meterRegistry.counter("techcellshop.orders.placed");
        this.orderPlaceConflictCounter = meterRegistry.counter("techcellshop.orders.place.conflict");
        this.orderIdempotencyHitCounter = meterRegistry.counter("techcellshop.orders.idempotency.hit");
        this.orderIdempotencyConflictCounter = meterRegistry.counter("techcellshop.orders.idempotency.conflict");
        this.orderPlaceTimer = meterRegistry.timer("techcellshop.orders.place.duration");
        this.couponApplyTimer = meterRegistry.timer("techcellshop.orders.coupon.apply.duration");
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

    @Retryable(
            retryFor = ObjectOptimisticLockingFailureException.class,
            backoff = @Backoff(delay = 100, multiplier = 2.0)
    )
    @Transactional
    @Override
    public Order placeOrder(OrderEnrollmentRequest request) {
        return orderPlaceTimer.record(() -> {
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

            BigDecimal total = MoneyUtils.multiply(device.getDevicePrice(), quantity);
            order.setTotalPriceOrder(total);
            order.setDiscountAmount(MoneyUtils.zero());
            order.setFinalAmount(total);

            Order saved = orderRepository.save(order);
            writeToOutbox(saved);

            orderPlacedCounter.increment();
            return saved;
        });
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
    public Order updateStatus(Long orderId, OrderStatus newStatus, String reason){
        if (newStatus.equals(OrderStatus.CANCELED)){
            return cancelOrder(orderId, reason);
        }

        Order order = getOrderOrThrow(orderId);
        orderStatusTransitionValidator.validateTransition(order.getStatus(), newStatus);

        if (newStatus.equals(OrderStatus.SHIPPED) && !order.getPaymentStatus().equals(PaymentStatus.CONFIRMED)){
            throw new InvalidOrderStatusTransitionException("Order cannot be shipped without confirmed payment");
        }

        order.setStatus(newStatus);
        return orderRepository.save(order);
    }

    @Transactional
    @Override
    public Order cancelOrder(Long orderId, String reason){
        Order order = getOrderOrThrow(orderId);
        if (order.getStatus().equals(OrderStatus.CANCELED)){
            return order;
        }

        if (order.getStatus().equals(OrderStatus.SHIPPED) || order.getStatus().equals(OrderStatus.DELIVERED)){
            throw new InvalidOrderStatusTransitionException("Cannot cancel an order that has already been shipped or delivered");
        }

        order.setStatus(OrderStatus.CANCELED);
        order.setCanceledReason((reason == null || reason.isBlank()) ? "Order canceled by the user" : reason);

        deviceService.releaseStock(order.getDevice().getIdDevice(), order.getQuantityOrder());
        return orderRepository.save(order);

    }

    @Transactional
    @Override
    public Order applyCoupon(Long orderId, String couponCode) {
        return couponApplyTimer.record(() -> {
            Order order = getOrderOrThrow(orderId);

            if (order.getStatus().equals(OrderStatus.SHIPPED)
                    || order.getStatus().equals(OrderStatus.DELIVERED)
                    || order.getStatus().equals(OrderStatus.CANCELED)) {
                throw new CouponValidationException("Coupon cannot be applied for orders that are in shipped, delivered or canceled status");
            } else if (order.getCouponCode() != null && !order.getCouponCode().isBlank()) {
                if (order.getCouponCode().equalsIgnoreCase(couponCode)) {
                    return order;
                }
                throw new CouponValidationException("An order can have only one coupon applied. Current applied coupon: " + order.getCouponCode());
            }

            BigDecimal orderAmount = MoneyUtils.normalize(order.getTotalPriceOrder());
            BigDecimal discount = MoneyUtils.normalize(couponService.calculateDiscount(couponCode, orderAmount));
            BigDecimal finalAmount = MoneyUtils.subtractFloorZero(orderAmount, discount);

            order.setCouponCode(couponCode);
            order.setDiscountAmount(discount);
            order.setFinalAmount(finalAmount);

            Order saved = orderRepository.save(order);
            couponService.registerCouponUsage(couponCode);
            return saved;
        });
    }

    @Transactional
    @Override
    public Order placeOrder(OrderEnrollmentRequest request, String idempotencyKey) {
        String normalisedIdempotencyKey = idempotencyKey.trim();

        var existing = orderIdempondencyRepository.findByIdempotencyKey(normalisedIdempotencyKey);
        if (existing.isPresent()){
            Order existingOrder = existing.get().getOrder();

            String currentHash = computeRequestHash(request);
            if (!existing.get().getRequestHash().equals(currentHash)){
                orderIdempotencyConflictCounter.increment();
                throw new IllegalStateException("Idempotency key already used for a different request");
            }

            orderIdempotencyHitCounter.increment();
            return existingOrder;
        }

        Order created = placeOrder(request);
        OrderIdempondency record = new OrderIdempondency();

        record.setIdempotencyKey(normalisedIdempotencyKey);
        record.setRequestHash(computeRequestHash(request));
        record.setOrder(created);
        record.setCreatedAt(OffsetDateTime.now());
        orderIdempondencyRepository.save(record);

        return created;
    }

    private void writeToOutbox(Order order) {
        OrderCreatedEvent event = new OrderCreatedEvent(
                UUID.randomUUID().toString(),
                order.getIdOrder(),
                order.getUser().getIdUser(),
                order.getDevice().getIdDevice(),
                order.getQuantityOrder(),
                order.getTotalPriceOrder(),
                order.getPaymentMethod(),
                order.getStatus(),
                order.getPaymentStatus(),
                Instant.now()
        );

        try {
            OutboxEvent outboxEvent = new OutboxEvent();
            outboxEvent.setEventId(event.eventId());
            outboxEvent.setEventType("order.created");
            outboxEvent.setAggregateType("Order");
            outboxEvent.setAggregateId(order.getIdOrder());
            outboxEvent.setPayload(objectMapper.writeValueAsString(event));
            outboxEvent.setStatus(OutboxEventStatus.PENDING);
            outboxEvent.setAttempts(0);
            outboxEvent.setNextAttemptAt(Instant.now());
            outboxEvent.setCreatedAt(Instant.now());

            outboxEventRepository.save(outboxEvent);
            log.info("Outbox event written for orderId={}", order.getIdOrder());

        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize outbox event for orderId=" + order.getIdOrder(), e);
        }
    }

    private String computeRequestHash(OrderEnrollmentRequest request) {
        String raw = request.getIdUser() + "|" +
                request.getIdDevice() + "|" +
                request.getQuantityOrder() + "|" +
                request.getPaymentMethod();

        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            byte[] hash = messageDigest.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available");
        }
    }

    @Recover
    public Order recoverFromOptimisticLock(ObjectOptimisticLockingFailureException ex, OrderEnrollmentRequest request) {
        orderPlaceConflictCounter.increment();
        throw ex;
    }
}
