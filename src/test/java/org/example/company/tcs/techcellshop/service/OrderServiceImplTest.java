package org.example.company.tcs.techcellshop.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.company.tcs.techcellshop.dto.request.OrderEnrollmentRequest;
import org.example.company.tcs.techcellshop.dto.request.OrderUpdateRequest;
import org.example.company.tcs.techcellshop.domain.*;
import org.example.company.tcs.techcellshop.exception.ResourceNotFoundException;
import org.example.company.tcs.techcellshop.mapper.RequestMapper;
import org.example.company.tcs.techcellshop.mapper.ResponseMapper;
import org.example.company.tcs.techcellshop.repository.*;
import org.example.company.tcs.techcellshop.service.impl.OrderServiceImpl;
import org.example.company.tcs.techcellshop.util.OrderStatus;
import org.example.company.tcs.techcellshop.util.PaymentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {


    @Mock private OrderRepository orderRepository;
    @Mock private RequestMapper requestMapper;
    @Mock private UserRepository userRepository;
    @Mock private DeviceRepository deviceRepository;
    @Mock private DeviceService deviceService;
    @Mock private OrderStatusTransitionValidator orderStatusTransitionValidator;
    @Mock private ResponseMapper responseMapper;
    @Mock private CouponService couponService;
    @Mock private OrderIdempondencyRepository orderIdempondencyRepository;
    @Mock private OutboxEventRepository outboxEventRepository;
    @Mock private ObjectMapper objectMapper;

    @InjectMocks
    private OrderServiceImpl orderService;

    private Order order;
    private User user;
    private Device device;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setIdUser(1L);
        user.setNameUser("Ana Silva");
        user.setEmailUser("ana@techcellshop.com");

        device = new Device();
        device.setIdDevice(1L);
        device.setNameDevice("Galaxy S24");
        device.setDevicePrice(3999.90);
        device.setDeviceStock(10);

        order = new Order();
        order.setIdOrder(1L);
        order.setUser(user);
        order.setDevice(device);
        order.setQuantityOrder(2);
        order.setTotalPriceOrder(7999.80);
        order.setStatus(OrderStatus.CREATED);
        order.setOrderDate("2026-03-24");
        order.setDeliveryDate("2026-03-31");
        order.setPaymentMethod("CREDIT_CARD");
        order.setPaymentStatus(PaymentStatus.PENDING);
    }

    @Nested
    @DisplayName("saveOrder")
    class SaveOrder {

        @Test
        @DisplayName("should save and return the order")
        void shouldSaveAndReturnOrder() {
            when(orderRepository.save(any(Order.class))).thenReturn(order);

            Order result = orderService.saveOrder(order);

            assertThat(result.getIdOrder()).isEqualTo(1L);
            assertThat(result.getStatus()).isEqualTo(OrderStatus.CREATED);   // enum, not String
            assertThat(result.getPaymentMethod()).isEqualTo("CREDIT_CARD");
            verify(orderRepository).save(order);
        }
    }

    @Nested
    @DisplayName("getOrderById")
    class GetOrderById {

        @Test
        @DisplayName("should return order when found")
        void shouldReturnOrderWhenFound() {
            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

            Order result = orderService.getOrderById(1L);

            assertThat(result.getIdOrder()).isEqualTo(1L);
            assertThat(result.getQuantityOrder()).isEqualTo(2);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when not found")
        void shouldThrowWhenNotFound() {
            when(orderRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.getOrderById(99L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("No order found with id: 99");
        }
    }

    @Nested
    @DisplayName("getAllOrders")
    class GetAllOrders {

        @Test
        @DisplayName("should return all orders")
        void shouldReturnAllOrders() {
            when(orderRepository.findAll()).thenReturn(List.of(order));

            List<Order> result = orderService.getAllOrders();

            assertThat(result).hasSize(1);
            assertThat(result.getFirst().getStatus()).isEqualTo(OrderStatus.CREATED); // enum
        }

        @Test
        @DisplayName("should return empty list when no orders exist")
        void shouldReturnEmptyList() {
            when(orderRepository.findAll()).thenReturn(List.of());

            assertThat(orderService.getAllOrders()).isEmpty();
        }
    }

    @Nested
    @DisplayName("updateOrder")
    class UpdateOrder {

        @Test
        @DisplayName("should apply update and return updated order")
        void shouldApplyUpdateAndReturn() {
            OrderUpdateRequest updateRequest = new OrderUpdateRequest();
            updateRequest.setQuantityOrder(3);
            updateRequest.setStatusOrder(OrderStatus.PAID);    // PAID exists in enum
            updateRequest.setDeliveryDate("2026-04-07");
            updateRequest.setPaymentStatus(PaymentStatus.CONFIRMED);  // realistic after update

            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

            doAnswer(invocation -> {
                Order target = invocation.getArgument(0);
                OrderUpdateRequest req = invocation.getArgument(1);
                target.setQuantityOrder(req.getQuantityOrder());
                target.setStatus(req.getStatusOrder());
                target.setDeliveryDate(req.getDeliveryDate());
                target.setPaymentStatus(req.getPaymentStatus());
                return null;
            }).when(requestMapper).updateOrder(any(Order.class), any(OrderUpdateRequest.class));

            when(orderRepository.save(any(Order.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            Order result = orderService.updateOrder(1L, updateRequest);

            assertThat(result.getQuantityOrder()).isEqualTo(3);
            assertThat(result.getStatus()).isEqualTo(OrderStatus.PAID);          // enum
            assertThat(result.getDeliveryDate()).isEqualTo("2026-04-07");
            assertThat(result.getPaymentStatus()).isEqualTo(PaymentStatus.CONFIRMED); // enum

            verify(requestMapper).updateOrder(order, updateRequest);
            verify(orderRepository).save(order);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when order not found")
        void shouldThrowWhenOrderNotFound() {
            OrderUpdateRequest updateRequest = new OrderUpdateRequest();
            updateRequest.setQuantityOrder(2);
            updateRequest.setStatusOrder(OrderStatus.SHIPPED);
            updateRequest.setDeliveryDate("2026-04-01");
            updateRequest.setPaymentStatus(PaymentStatus.CONFIRMED);

            when(orderRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.updateOrder(99L, updateRequest))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("No order found with id: 99");

            verify(requestMapper, never()).updateOrder(any(), any());
            verify(orderRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("deleteOrder")
    class DeleteOrder {

        @Test
        @DisplayName("should delete when order exists")
        void shouldDeleteWhenExists() {
            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

            orderService.deleteOrder(1L);

            verify(orderRepository).delete(order);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException and never delete when not found")
        void shouldThrowAndNeverDelete() {
            when(orderRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.deleteOrder(99L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("No order found with id: 99");

            verify(orderRepository, never()).delete(any());
        }
    }

    @Nested
    @DisplayName("placeOrder (with Idempotency-Key)")
    class PlaceOrderIdempotency {

        private OrderEnrollmentRequest request;

        @BeforeEach
        void setUpRequest() {
            request = new OrderEnrollmentRequest();
            request.setIdUser(1L);
            request.setIdDevice(1L);
            request.setQuantityOrder(1);
            request.setPaymentMethod("PIX");
        }

        @Test
        @DisplayName("new key: should create order and store idempotency record")
        void shouldCreateOrderAndStoreRecord_whenKeyIsNew() throws Exception {
            when(orderIdempondencyRepository.findByIdempotencyKey("KEY-001"))
                    .thenReturn(Optional.empty());
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(deviceRepository.findById(1L)).thenReturn(Optional.of(device));
            doNothing().when(deviceService).reserveStock(any(), any());
            when(orderRepository.save(any(Order.class))).thenReturn(order);

            // Stub objectMapper so writeToOutbox() doesn't throw
            when(objectMapper.writeValueAsString(any())).thenReturn("{\"eventId\":\"test\"}");
            when(outboxEventRepository.save(any(OutboxEvent.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(orderIdempondencyRepository.save(any(OrderIdempondency.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            Order result = orderService.placeOrder(request, "KEY-001");

            assertThat(result.getIdOrder()).isEqualTo(1L);
            verify(outboxEventRepository).save(any(OutboxEvent.class));  // outbox written
            verify(orderIdempondencyRepository).save(any(OrderIdempondency.class));
        }

        @Test
        @DisplayName("existing key + same payload: should return existing order without creating a new one")
        void shouldReturnExistingOrder_whenKeyAlreadyExists() {
            String rawHash = "1|1|1|PIX";
            String expectedHash = sha256(rawHash);

            OrderIdempondency existingRecord = new OrderIdempondency();
            existingRecord.setIdempotencyKey("KEY-001");
            existingRecord.setRequestHash(expectedHash);
            existingRecord.setOrder(order);

            when(orderIdempondencyRepository.findByIdempotencyKey("KEY-001"))
                    .thenReturn(Optional.of(existingRecord));

            Order result = orderService.placeOrder(request, "KEY-001");

            assertThat(result.getIdOrder()).isEqualTo(1L);

            verify(userRepository, never()).findById(any());
            verify(orderRepository, never()).save(any());
            verify(orderIdempondencyRepository, never()).save(any());
        }

        @Test
        @DisplayName("existing key + different payload: should throw IllegalStateException")
        void shouldThrow_whenKeyExistsWithDifferentPayload() {
            OrderIdempondency existingRecord = new OrderIdempondency();
            existingRecord.setIdempotencyKey("KEY-001");
            existingRecord.setRequestHash("completely-different-hash-0000");
            existingRecord.setOrder(order);

            when(orderIdempondencyRepository.findByIdempotencyKey("KEY-001"))
                    .thenReturn(Optional.of(existingRecord));

            assertThatThrownBy(() -> orderService.placeOrder(request, "KEY-001"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Idempotency key already used for a different request");

            verify(userRepository, never()).findById(any());
            verify(orderRepository, never()).save(any());
        }

        private String sha256(String input) {
            try {
                java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
                byte[] hash = md.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                return java.util.HexFormat.of().formatHex(hash);
            } catch (java.security.NoSuchAlgorithmException e) {
                throw new IllegalStateException("SHA-256 not available", e);
            }
        }
    }
}