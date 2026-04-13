package org.example.company.tcs.techcellshop.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.example.company.tcs.techcellshop.domain.Device;
import org.example.company.tcs.techcellshop.domain.Order;
import org.example.company.tcs.techcellshop.domain.OrderIdempondency;
import org.example.company.tcs.techcellshop.domain.OutboxEvent;
import org.example.company.tcs.techcellshop.domain.User;
import org.example.company.tcs.techcellshop.dto.request.OrderEnrollmentRequest;
import org.example.company.tcs.techcellshop.dto.request.OrderUpdateRequest;
import org.example.company.tcs.techcellshop.exception.ResourceNotFoundException;
import org.example.company.tcs.techcellshop.mapper.RequestMapper;
import org.example.company.tcs.techcellshop.repository.DeviceRepository;
import org.example.company.tcs.techcellshop.repository.OrderIdempondencyRepository;
import org.example.company.tcs.techcellshop.repository.OrderRepository;
import org.example.company.tcs.techcellshop.repository.OutboxEventRepository;
import org.example.company.tcs.techcellshop.repository.UserRepository;
import org.example.company.tcs.techcellshop.service.impl.OrderServiceImpl;
import org.example.company.tcs.techcellshop.service.impl.order.ApplyCouponToOrder;
import org.example.company.tcs.techcellshop.service.impl.order.CancelOrder;
import org.example.company.tcs.techcellshop.service.impl.order.PlaceOrder;
import org.example.company.tcs.techcellshop.service.impl.order.UpdateOrderStatus;
import org.example.company.tcs.techcellshop.util.OrderStatus;
import org.example.company.tcs.techcellshop.util.PaymentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.lang.reflect.Constructor;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private RequestMapper requestMapper;

    @Mock
    private PlaceOrder placeOrder;

    @Mock
    private UpdateOrderStatus updateOrderStatus;

    @Mock
    private CancelOrder cancelOrder;

    @Mock
    private ApplyCouponToOrder applyCouponToOrder;

    private OrderServiceImpl orderService;

    private Order order;
    private User user;
    private Device device;

    @BeforeEach
    void setUp() {
        orderService = newOrderService();

        user = new User();
        user.setIdUser(1L);
        user.setNameUser("Ana Silva");
        user.setEmailUser("ana@techcellshop.com");

        device = new Device();
        device.setIdDevice(1L);
        device.setNameDevice("Galaxy S24");
        device.setDevicePrice(money("3999.90"));
        device.setDeviceStock(10);

        order = new Order();
        order.setIdOrder(1L);
        order.setUser(user);
        order.setDevice(device);
        order.setQuantityOrder(2);
        order.setTotalPriceOrder(money("7999.80"));
        order.setDiscountAmount(money("0.00"));
        order.setFinalAmount(money("7999.80"));
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
            assertThat(result.getStatus()).isEqualTo(OrderStatus.CREATED);
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
            Page<Order> page = new PageImpl<>(List.of(order), PageRequest.of(0, 20), 1);
            when(orderRepository.findAll(any(Pageable.class))).thenReturn(page);

            Page<Order> result = orderService.getAllOrders(PageRequest.of(0, 20));

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().getFirst().getStatus()).isEqualTo(OrderStatus.CREATED);
        }

        @Test
        @DisplayName("should return empty page when no orders exist")
        void shouldReturnEmptyPageWhenNoOrdersExist() {
            when(orderRepository.findAll(any(Pageable.class)))
                    .thenReturn(Page.empty(PageRequest.of(0, 20)));

            Page<Order> result = orderService.getAllOrders(PageRequest.of(0, 20));

            assertThat(result.getContent()).isEmpty();
        }
    }

    @Nested
    @DisplayName("getOrdersForUser")
    class GetOrdersForUser {

        @Test
        @DisplayName("should return orders for the given user email")
        void shouldReturnOrdersForUser() {
            Page<Order> page = new PageImpl<>(List.of(order), PageRequest.of(0, 20), 1);
            when(orderRepository.findByUser_EmailUserIgnoreCase(eq("ana@techcellshop.com"), any(Pageable.class)))
                    .thenReturn(page);

            Page<Order> result = orderService.getOrdersForUser("ana@techcellshop.com", PageRequest.of(0, 20));

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().getFirst().getUser().getEmailUser())
                    .isEqualTo("ana@techcellshop.com");
        }

        @Test
        @DisplayName("should return empty page when user has no orders")
        void shouldReturnEmptyPageWhenUserHasNoOrders() {
            when(orderRepository.findByUser_EmailUserIgnoreCase(eq("ana@techcellshop.com"), any(Pageable.class)))
                    .thenReturn(Page.empty(PageRequest.of(0, 20)));

            Page<Order> result = orderService.getOrdersForUser("ana@techcellshop.com", PageRequest.of(0, 20));

            assertThat(result.getContent()).isEmpty();
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
            updateRequest.setStatusOrder(OrderStatus.PAID);
            updateRequest.setDeliveryDate("2026-04-07");
            updateRequest.setPaymentStatus(PaymentStatus.CONFIRMED);

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
            assertThat(result.getStatus()).isEqualTo(OrderStatus.PAID);
            assertThat(result.getDeliveryDate()).isEqualTo("2026-04-07");
            assertThat(result.getPaymentStatus()).isEqualTo(PaymentStatus.CONFIRMED);

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
    @DisplayName("placeOrder")
    class PlaceOrderDelegation {

        @Test
        @DisplayName("should delegate non-idempotent placeOrder to PlaceOrder")
        void shouldDelegateNonIdempotentPlaceOrder() {
            OrderEnrollmentRequest request = new OrderEnrollmentRequest();
            request.setIdDevice(1L);
            request.setQuantityOrder(1);
            request.setPaymentMethod("PIX");

            when(placeOrder.placeOrder(request, "ana@techcellshop.com")).thenReturn(order);

            Order result = orderService.placeOrder(request, "ana@techcellshop.com");

            assertThat(result).isSameAs(order);

            verify(placeOrder).placeOrder(request, "ana@techcellshop.com");
        }

        @Test
        @DisplayName("should delegate idempotent placeOrder to PlaceOrder")
        void shouldDelegateIdempotentPlaceOrder() {
            OrderEnrollmentRequest request = new OrderEnrollmentRequest();
            request.setIdDevice(1L);
            request.setQuantityOrder(1);
            request.setPaymentMethod("PIX");

            when(placeOrder.placeOrder(request, "ana@techcellshop.com", "KEY-001")).thenReturn(order);

            Order result = orderService.placeOrder(request, "ana@techcellshop.com", "KEY-001");

            assertThat(result).isSameAs(order);

            verify(placeOrder).placeOrder(request, "ana@techcellshop.com", "KEY-001");
        }
    }

    @Nested
    @DisplayName("updateStatus")
    class UpdateStatusDelegation {

        @Test
        @DisplayName("should delegate updateStatus to UpdateOrderStatus")
        void shouldDelegateUpdateStatus() {
            Order paidOrder = new Order();
            paidOrder.setIdOrder(1L);
            paidOrder.setStatus(OrderStatus.PAID);

            when(updateOrderStatus.updateStatus(1L, OrderStatus.PAID, "payment confirmed"))
                    .thenReturn(paidOrder);

            Order result = orderService.updateStatus(1L, OrderStatus.PAID, "payment confirmed");

            assertThat(result).isSameAs(paidOrder);
            assertThat(result.getStatus()).isEqualTo(OrderStatus.PAID);

            verify(updateOrderStatus).updateStatus(1L, OrderStatus.PAID, "payment confirmed");
        }
    }

    @Nested
    @DisplayName("cancelOrder")
    class CancelOrderDelegation {

        @Test
        @DisplayName("should delegate cancelOrder to CancelOrder")
        void shouldDelegateCancelOrder() {
            Order canceledOrder = new Order();
            canceledOrder.setIdOrder(1L);
            canceledOrder.setStatus(OrderStatus.CANCELED);
            canceledOrder.setCanceledReason("Customer request");

            when(cancelOrder.cancelOrder(1L, "Customer request")).thenReturn(canceledOrder);

            Order result = orderService.cancelOrder(1L, "Customer request");

            assertThat(result).isSameAs(canceledOrder);
            assertThat(result.getStatus()).isEqualTo(OrderStatus.CANCELED);
            assertThat(result.getCanceledReason()).isEqualTo("Customer request");

            verify(cancelOrder).cancelOrder(1L, "Customer request");
        }
    }

    @Nested
    @DisplayName("applyCoupon")
    class ApplyCouponDelegation {

        @Test
        @DisplayName("should delegate applyCoupon to ApplyCouponToOrder")
        void shouldDelegateApplyCoupon() {
            Order discountedOrder = new Order();
            discountedOrder.setIdOrder(1L);
            discountedOrder.setCouponCode("WELCOME10");
            discountedOrder.setDiscountAmount(money("10.00"));
            discountedOrder.setFinalAmount(money("7989.80"));

            when(applyCouponToOrder.applyCoupon(1L, "WELCOME10")).thenReturn(discountedOrder);

            Order result = orderService.applyCoupon(1L, "WELCOME10");

            assertThat(result).isSameAs(discountedOrder);
            assertThat(result.getCouponCode()).isEqualTo("WELCOME10");
            assertThat(result.getDiscountAmount()).isEqualByComparingTo("10.00");
            assertThat(result.getFinalAmount()).isEqualByComparingTo("7989.80");

            verify(applyCouponToOrder).applyCoupon(1L, "WELCOME10");
        }
    }

    private OrderServiceImpl newOrderService() {
        try {
            Constructor<OrderServiceImpl> constructor = OrderServiceImpl.class.getDeclaredConstructor(
                    OrderRepository.class,
                    RequestMapper.class,
                    PlaceOrder.class,
                    UpdateOrderStatus.class,
                    CancelOrder.class,
                    ApplyCouponToOrder.class
            );
            constructor.setAccessible(true);
            return constructor.newInstance(
                    orderRepository,
                    requestMapper,
                    placeOrder,
                    updateOrderStatus,
                    cancelOrder,
                    applyCouponToOrder
            );
        } catch (ReflectiveOperationException ex) {
            throw new RuntimeException(ex);
        }
    }

    private BigDecimal money(String value) {
        return new BigDecimal(value);
    }
}
