package org.example.company.tcs.techcellshop.service;

import org.example.company.tcs.techcellshop.controller.dto.request.OrderUpdateRequest;
import org.example.company.tcs.techcellshop.domain.Device;
import org.example.company.tcs.techcellshop.domain.Order;
import org.example.company.tcs.techcellshop.domain.User;
import org.example.company.tcs.techcellshop.exception.ResourceNotFoundException;
import org.example.company.tcs.techcellshop.mapper.RequestMapper;
import org.example.company.tcs.techcellshop.repository.OrderRepository;
import org.example.company.tcs.techcellshop.service.impl.OrderServiceImpl;
import org.example.company.tcs.techcellshop.util.OrderStatus;
import org.example.company.tcs.techcellshop.util.PaymentStatus;
import org.junit.jupiter.api.BeforeEach;
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

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private RequestMapper requestMapper;

    @InjectMocks
    private OrderServiceImpl orderService;

    private Order order;

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setIdUser(1L);
        user.setNameUser("Ana Silva");
        user.setEmailUser("ana@techcellshop.com");

        Device device = new Device();
        device.setIdDevice(1L);
        device.setNameDevice("Galaxy S24");
        device.setDevicePrice(3999.90);

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

    @Test
    void saveOrder_shouldSaveAndReturnOrder() {
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        Order result = orderService.saveOrder(order);

        assertThat(result.getIdOrder()).isEqualTo(1L);
        assertThat(result.getStatus()).isEqualTo("CREATED");
        assertThat(result.getPaymentMethod()).isEqualTo("CREDIT_CARD");
        verify(orderRepository).save(order);
    }

    @Test
    void getOrderById_whenOrderExists_shouldReturnOrder() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        Order result = orderService.getOrderById(1L);

        assertThat(result.getIdOrder()).isEqualTo(1L);
        assertThat(result.getQuantityOrder()).isEqualTo(2);
    }

    @Test
    void getOrderById_whenOrderNotFound_shouldThrowResourceNotFoundException() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrderById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Order not found with id: 99");
    }

    @Test
    void getAllOrders_shouldReturnAllOrders() {
        when(orderRepository.findAll()).thenReturn(List.of(order));

        List<Order> result = orderService.getAllOrders();

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getStatus()).isEqualTo("CREATED");
    }

    @Test
    void getAllOrders_whenEmpty_shouldReturnEmptyList() {
        when(orderRepository.findAll()).thenReturn(List.of());

        List<Order> result = orderService.getAllOrders();

        assertThat(result).isEmpty();
    }

    @Test
    void updateOrder_whenOrderExists_shouldApplyUpdateRequestAndReturn() {
        OrderUpdateRequest updateRequest = new OrderUpdateRequest();
        updateRequest.setQuantityOrder(3);
        updateRequest.setStatusOrder(OrderStatus.PAID);
        updateRequest.setDeliveryDate("2026-04-07");
        updateRequest.setPaymentStatus(PaymentStatus.AUTHORIZED);

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

        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Order result = orderService.updateOrder(1L, updateRequest);

        assertThat(result.getQuantityOrder()).isEqualTo(3);
        assertThat(result.getStatus()).isEqualTo("PROCESSING");
        assertThat(result.getDeliveryDate()).isEqualTo("2026-04-07");
        assertThat(result.getPaymentStatus()).isEqualTo("PAID");

        verify(requestMapper).updateOrder(order, updateRequest);
        verify(orderRepository).save(order);
    }

    @Test
    void updateOrder_whenOrderNotFound_shouldThrowResourceNotFoundException() {
        OrderUpdateRequest updateRequest = new OrderUpdateRequest();
        updateRequest.setQuantityOrder(2);
        updateRequest.setStatusOrder(OrderStatus.SHIPPED);
        updateRequest.setDeliveryDate("2026-04-01");
        updateRequest.setPaymentStatus(PaymentStatus.AUTHORIZED);

        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.updateOrder(99L, updateRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Order not found with id: 99");

        verify(requestMapper, never()).updateOrder(any(Order.class), any(OrderUpdateRequest.class));
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void deleteOrder_whenOrderExists_shouldCallRepositoryDelete() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        orderService.deleteOrder(1L);

        verify(orderRepository).delete(order);
    }

    @Test
    void deleteOrder_whenOrderNotFound_shouldThrowResourceNotFoundExceptionAndNeverDelete() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.deleteOrder(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Order not found with id: 99");

        verify(orderRepository, never()).delete(any());
    }
}
