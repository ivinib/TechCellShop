package org.example.company.tcs.techcellshop.service;

import org.example.company.tcs.techcellshop.domain.Device;
import org.example.company.tcs.techcellshop.domain.Order;
import org.example.company.tcs.techcellshop.domain.User;
import org.example.company.tcs.techcellshop.exception.ResourceNotFoundException;
import org.example.company.tcs.techcellshop.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

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
        order.setStatusOrder("CREATED");
        order.setOrderDate("2026-03-24");
        order.setDeliveryDate("2026-03-31");
        order.setPaymentMethod("CREDIT_CARD");
        order.setPaymentStatus("PENDING");
    }

    @Test
    void saveOrder_shouldSaveAndReturnOrder() {
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        Order result = orderService.saveOrder(order);

        assertThat(result.getIdOrder()).isEqualTo(1L);
        assertThat(result.getStatusOrder()).isEqualTo("CREATED");
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
        assertThat(result.get(0).getStatusOrder()).isEqualTo("CREATED");
    }

    @Test
    void getAllOrders_whenEmpty_shouldReturnEmptyList() {
        when(orderRepository.findAll()).thenReturn(List.of());

        List<Order> result = orderService.getAllOrders();

        assertThat(result).isEmpty();
    }

    @Test
    void updateOrder_whenOrderExists_shouldUpdateAllFieldsAndReturn() {
        Order updateData = new Order();
        updateData.setUser(order.getUser());
        updateData.setDevice(order.getDevice());
        updateData.setQuantityOrder(3);
        updateData.setTotalPriceOrder(11999.70);
        updateData.setStatusOrder("PROCESSING");
        updateData.setOrderDate("2026-03-24");
        updateData.setDeliveryDate("2026-04-07");
        updateData.setPaymentMethod("PIX");
        updateData.setPaymentStatus("PAID");

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        Order result = orderService.updateOrder(1L, updateData);

        assertThat(result.getQuantityOrder()).isEqualTo(3);
        assertThat(result.getTotalPriceOrder()).isEqualTo(11999.70);
        assertThat(result.getStatusOrder()).isEqualTo("PROCESSING");
        assertThat(result.getPaymentMethod()).isEqualTo("PIX");
        assertThat(result.getPaymentStatus()).isEqualTo("PAID");
        verify(orderRepository).save(order);
    }

    @Test
    void updateOrder_whenOrderNotFound_shouldThrowResourceNotFoundException() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.updateOrder(99L, new Order()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Order not found with id: 99");
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
