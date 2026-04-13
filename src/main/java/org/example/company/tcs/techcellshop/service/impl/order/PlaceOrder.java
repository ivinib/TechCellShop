package org.example.company.tcs.techcellshop.service.impl.order;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.transaction.Transactional;
import org.example.company.tcs.techcellshop.domain.Device;
import org.example.company.tcs.techcellshop.domain.Order;
import org.example.company.tcs.techcellshop.domain.OrderIdempondency;
import org.example.company.tcs.techcellshop.domain.User;
import org.example.company.tcs.techcellshop.dto.request.OrderEnrollmentRequest;
import org.example.company.tcs.techcellshop.exception.ResourceNotFoundException;
import org.example.company.tcs.techcellshop.repository.DeviceRepository;
import org.example.company.tcs.techcellshop.repository.OrderIdempondencyRepository;
import org.example.company.tcs.techcellshop.repository.OrderRepository;
import org.example.company.tcs.techcellshop.repository.UserRepository;
import org.example.company.tcs.techcellshop.service.DeviceService;
import org.example.company.tcs.techcellshop.util.MoneyUtils;
import org.example.company.tcs.techcellshop.util.OrderStatus;
import org.example.company.tcs.techcellshop.util.PaymentStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.HexFormat;

import static org.example.company.tcs.techcellshop.util.AppConstants.DEVICE_NOT_FOUND;

@Component
public class PlaceOrder {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final DeviceRepository deviceRepository;
    private final DeviceService deviceService;
    private final OrderIdempondencyRepository orderIdempondencyRepository;
    private final OrderOutboxWriter orderOutboxWriter;

    private final Counter orderPlacedCounter;
    private final Counter orderPlaceConflictCounter;
    private final Counter orderIdempotencyHitCounter;
    private final Counter orderIdempotencyConflictCounter;
    private final Timer orderPlaceTimer;

    public PlaceOrder(
            OrderRepository orderRepository,
            UserRepository userRepository,
            DeviceRepository deviceRepository,
            DeviceService deviceService,
            OrderIdempondencyRepository orderIdempondencyRepository,
            OrderOutboxWriter orderOutboxWriter,
            MeterRegistry meterRegistry
    ) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.deviceRepository = deviceRepository;
        this.deviceService = deviceService;
        this.orderIdempondencyRepository = orderIdempondencyRepository;
        this.orderOutboxWriter = orderOutboxWriter;

        this.orderPlacedCounter = meterRegistry.counter("techcellshop.orders.placed");
        this.orderPlaceConflictCounter = meterRegistry.counter("techcellshop.orders.place.conflict");
        this.orderIdempotencyHitCounter = meterRegistry.counter("techcellshop.orders.idempotency.hit");
        this.orderIdempotencyConflictCounter = meterRegistry.counter("techcellshop.orders.idempotency.conflict");
        this.orderPlaceTimer = meterRegistry.timer("techcellshop.orders.place.duration");
    }

    @Transactional
    public Order placeOrder(OrderEnrollmentRequest request, String authenticatedEmail) {
        return doPlaceOrder(request, authenticatedEmail);
    }

    @Retryable(
            retryFor = ObjectOptimisticLockingFailureException.class,
            backoff = @Backoff(delay = 100, multiplier = 2.0)
    )
    @Transactional
    public Order placeOrder(
            OrderEnrollmentRequest request,
            String authenticatedEmail,
            String idempotencyKey
    ) {
        return orderPlaceTimer.record(() -> {
            String normalizedIdempotencyKey = idempotencyKey.trim();
            String currentHash = computeRequestHash(request, authenticatedEmail);

            var existing = orderIdempondencyRepository.findByIdempotencyKey(normalizedIdempotencyKey);
            if (existing.isPresent()) {
                Order existingOrder = existing.get().getOrder();

                if (!existing.get().getRequestHash().equals(currentHash)) {
                    orderIdempotencyConflictCounter.increment();
                    throw new IllegalStateException(
                            "Idempotency key already used for a different request"
                    );
                }

                orderIdempotencyHitCounter.increment();
                return existingOrder;
            }

            Order created = doPlaceOrder(request, authenticatedEmail);

            OrderIdempondency record = new OrderIdempondency();
            record.setIdempotencyKey(normalizedIdempotencyKey);
            record.setRequestHash(currentHash);
            record.setOrder(created);
            record.setCreatedAt(OffsetDateTime.now());
            orderIdempondencyRepository.save(record);

            return created;
        });
    }

    private Order doPlaceOrder(OrderEnrollmentRequest request, String authenticatedEmail) {
        User user = getAuthenticatedUserOrThrow(authenticatedEmail);

        Device device = deviceRepository.findById(request.getIdDevice())
                .orElseThrow(() -> new ResourceNotFoundException(
                        DEVICE_NOT_FOUND + request.getIdDevice()
                ));

        Integer quantity = request.getQuantityOrder();
        deviceService.reserveStock(device.getIdDevice(), quantity);

        Order order = new Order();
        order.setUser(user);
        order.setDevice(device);

        order.setUserIdSnapshot(user.getIdUser());
        order.setUserNameSnapshot(user.getNameUser());
        order.setUserEmailSnapshot(user.getEmailUser());

        order.setDeviceIdSnapshot(device.getIdDevice());
        order.setDeviceNameSnapshot(device.getNameDevice());
        order.setUnitPriceSnapshot(device.getDevicePrice());

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
        orderOutboxWriter.writeOrderCreated(saved);

        orderPlacedCounter.increment();
        return saved;
    }

    private User getAuthenticatedUserOrThrow(String authenticatedEmail) {
        if (authenticatedEmail == null || authenticatedEmail.isBlank()) {
            throw new IllegalArgumentException("Authenticated user email is required");
        }

        return userRepository.findByEmailUserIgnoreCase(authenticatedEmail)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found for authenticated principal: " + authenticatedEmail
                ));
    }

    private String computeRequestHash(OrderEnrollmentRequest request, String authenticatedEmail) {
        String raw = authenticatedEmail + "|"
                + request.getIdDevice() + "|"
                + request.getQuantityOrder() + "|"
                + request.getPaymentMethod();

        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            byte[] hash = messageDigest.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 not available");
        }
    }

    @Recover
    public Order recoverFromOptimisticLock(
            ObjectOptimisticLockingFailureException ex,
            OrderEnrollmentRequest request,
            String authenticatedEmail,
            String idempotencyKey
    ) {
        orderPlaceConflictCounter.increment();
        throw ex;
    }
}
