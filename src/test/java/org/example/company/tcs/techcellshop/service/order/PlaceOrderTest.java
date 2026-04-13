package org.example.company.tcs.techcellshop.service.order;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
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
import org.example.company.tcs.techcellshop.service.impl.order.OrderOutboxWriter;
import org.example.company.tcs.techcellshop.service.impl.order.PlaceOrder;
import org.example.company.tcs.techcellshop.util.OrderStatus;
import org.example.company.tcs.techcellshop.util.PaymentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PlaceOrderTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private DeviceRepository deviceRepository;

    @Mock
    private DeviceService deviceService;

    @Mock
    private OrderIdempondencyRepository orderIdempondencyRepository;

    @Mock
    private OrderOutboxWriter orderOutboxWriter;

    @Mock
    private MeterRegistry meterRegistry;

    @Mock
    private Counter counter;

    @Mock
    private Timer timer;

    private PlaceOrder placeOrder;

    private User user;
    private Device device;
    private OrderEnrollmentRequest request;

    @BeforeEach
    void setUp() {
        when(meterRegistry.counter(anyString())).thenReturn(counter);
        when(meterRegistry.timer(anyString())).thenReturn(timer);

        doAnswer(invocation -> {
            Supplier<?> supplier = invocation.getArgument(0);
            return supplier.get();
        }).when(timer).record(org.mockito.ArgumentMatchers.<Supplier<?>>any());

        placeOrder = new PlaceOrder(
                orderRepository,
                userRepository,
                deviceRepository,
                deviceService,
                orderIdempondencyRepository,
                orderOutboxWriter,
                meterRegistry
        );

        user = new User();
        user.setIdUser(1L);
        user.setNameUser("Ana Silva");
        user.setEmailUser("ana@techcellshop.com");

        device = new Device();
        device.setIdDevice(1L);
        device.setNameDevice("Galaxy S24");
        device.setDevicePrice(money("3999.90"));
        device.setDeviceStock(10);

        request = new OrderEnrollmentRequest();
        request.setIdDevice(1L);
        request.setQuantityOrder(1);
        request.setPaymentMethod("PIX");
    }

    @Nested
    @DisplayName("placeOrder without idempotency")
    class PlaceOrderWithoutIdempotency {

        @Test
        @DisplayName("should create order successfully")
        void shouldCreateOrderSuccessfully() {
            when(userRepository.findByEmailUserIgnoreCase("ana@techcellshop.com"))
                    .thenReturn(Optional.of(user));
            when(deviceRepository.findById(1L)).thenReturn(Optional.of(device));
            doNothing().when(deviceService).reserveStock(1L, 1);

            when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
                Order saved = invocation.getArgument(0);
                saved.setIdOrder(1L);
                return saved;
            });

            Order result = placeOrder.placeOrder(request, "ana@techcellshop.com");

            assertThat(result.getIdOrder()).isEqualTo(1L);
            assertThat(result.getUser()).isEqualTo(user);
            assertThat(result.getDevice()).isEqualTo(device);

            assertThat(result.getUserIdSnapshot()).isEqualTo(1L);
            assertThat(result.getUserNameSnapshot()).isEqualTo("Ana Silva");
            assertThat(result.getUserEmailSnapshot()).isEqualTo("ana@techcellshop.com");

            assertThat(result.getDeviceIdSnapshot()).isEqualTo(1L);
            assertThat(result.getDeviceNameSnapshot()).isEqualTo("Galaxy S24");
            assertThat(result.getUnitPriceSnapshot()).isEqualByComparingTo("3999.90");

            assertThat(result.getStatus()).isEqualTo(OrderStatus.CREATED);
            assertThat(result.getPaymentStatus()).isEqualTo(PaymentStatus.PENDING);
            assertThat(result.getTotalPriceOrder()).isEqualByComparingTo("3999.90");
            assertThat(result.getDiscountAmount()).isEqualByComparingTo("0.00");
            assertThat(result.getFinalAmount()).isEqualByComparingTo("3999.90");

            verify(deviceService).reserveStock(1L, 1);
            verify(orderOutboxWriter).writeOrderCreated(result);
            verify(counter).increment();
        }

        @Test
        @DisplayName("should throw when authenticated email is blank")
        void shouldThrowWhenAuthenticatedEmailIsBlank() {
            assertThatThrownBy(() -> placeOrder.placeOrder(request, " "))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Authenticated user email is required");
        }

        @Test
        @DisplayName("should throw when user is not found")
        void shouldThrowWhenUserIsNotFound() {
            when(userRepository.findByEmailUserIgnoreCase("ana@techcellshop.com"))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> placeOrder.placeOrder(request, "ana@techcellshop.com"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("User not found for authenticated principal: ana@techcellshop.com");
        }

        @Test
        @DisplayName("should throw when device is not found")
        void shouldThrowWhenDeviceIsNotFound() {
            when(userRepository.findByEmailUserIgnoreCase("ana@techcellshop.com"))
                    .thenReturn(Optional.of(user));
            when(deviceRepository.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> placeOrder.placeOrder(request, "ana@techcellshop.com"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("No device found with id: 1");
        }
    }

    @Nested
    @DisplayName("placeOrder with idempotency")
    class PlaceOrderWithIdempotency {

        @Test
        @DisplayName("new key should create order and store idempotency record")
        void shouldCreateOrderAndStoreRecordWhenKeyIsNew() {
            when(orderIdempondencyRepository.findByIdempotencyKey("KEY-001"))
                    .thenReturn(Optional.empty());
            when(userRepository.findByEmailUserIgnoreCase("ana@techcellshop.com"))
                    .thenReturn(Optional.of(user));
            when(deviceRepository.findById(1L)).thenReturn(Optional.of(device));
            doNothing().when(deviceService).reserveStock(1L, 1);

            when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
                Order saved = invocation.getArgument(0);
                saved.setIdOrder(1L);
                return saved;
            });

            when(orderIdempondencyRepository.save(any(OrderIdempondency.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            Order result = placeOrder.placeOrder(request, "ana@techcellshop.com", "KEY-001");

            assertThat(result.getIdOrder()).isEqualTo(1L);

            assertThat(result.getUserIdSnapshot()).isEqualTo(1L);
            assertThat(result.getUserNameSnapshot()).isEqualTo("Ana Silva");
            assertThat(result.getUserEmailSnapshot()).isEqualTo("ana@techcellshop.com");

            assertThat(result.getDeviceIdSnapshot()).isEqualTo(1L);
            assertThat(result.getDeviceNameSnapshot()).isEqualTo("Galaxy S24");
            assertThat(result.getUnitPriceSnapshot()).isEqualByComparingTo("3999.90");

            assertThat(result.getTotalPriceOrder()).isEqualByComparingTo("3999.90");
            assertThat(result.getDiscountAmount()).isEqualByComparingTo("0.00");
            assertThat(result.getFinalAmount()).isEqualByComparingTo("3999.90");

            verify(deviceService).reserveStock(1L, 1);
            verify(orderOutboxWriter).writeOrderCreated(result);
            verify(orderIdempondencyRepository).save(any(OrderIdempondency.class));
            verify(counter).increment();
        }

        @Test
        @DisplayName("existing key with same payload should return existing order")
        void shouldReturnExistingOrderWhenKeyAlreadyExists() {
            Order existingOrder = new Order();
            existingOrder.setIdOrder(1L);

            OrderIdempondency existingRecord = new OrderIdempondency();
            existingRecord.setIdempotencyKey("KEY-001");
            existingRecord.setRequestHash(sha256("ana@techcellshop.com|1|1|PIX"));
            existingRecord.setOrder(existingOrder);

            when(orderIdempondencyRepository.findByIdempotencyKey("KEY-001"))
                    .thenReturn(Optional.of(existingRecord));

            Order result = placeOrder.placeOrder(request, "ana@techcellshop.com", "KEY-001");

            assertThat(result.getIdOrder()).isEqualTo(1L);

            verify(userRepository, never()).findByEmailUserIgnoreCase(anyString());
            verify(orderRepository, never()).save(any());
            verify(orderIdempondencyRepository, never()).save(any(OrderIdempondency.class));
        }

        @Test
        @DisplayName("existing key with different payload should throw")
        void shouldThrowWhenKeyExistsWithDifferentPayload() {
            OrderIdempondency existingRecord = new OrderIdempondency();
            existingRecord.setIdempotencyKey("KEY-001");
            existingRecord.setRequestHash("different-hash");
            existingRecord.setOrder(new Order());

            when(orderIdempondencyRepository.findByIdempotencyKey("KEY-001"))
                    .thenReturn(Optional.of(existingRecord));

            assertThatThrownBy(() -> placeOrder.placeOrder(request, "ana@techcellshop.com", "KEY-001"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Idempotency key already used for a different request");
        }
    }

    @Test
    @DisplayName("recoverFromOptimisticLock should increment conflict counter and rethrow")
    void recoverFromOptimisticLockShouldIncrementCounterAndRethrow() {
        ObjectOptimisticLockingFailureException exception =
                new ObjectOptimisticLockingFailureException(Order.class, 1L);

        assertThatThrownBy(() ->
                placeOrder.recoverFromOptimisticLock(exception, request, "ana@techcellshop.com", "KEY-001"))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);

        verify(counter).increment();
    }

    private String sha256(String input) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(hash);
        } catch (java.security.NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 not available", ex);
        }
    }

    private BigDecimal money(String value) {
        return new BigDecimal(value);
    }
}
