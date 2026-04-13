package org.example.company.tcs.techcellshop.service.order;

import org.example.company.tcs.techcellshop.domain.Device;
import org.example.company.tcs.techcellshop.domain.Order;
import org.example.company.tcs.techcellshop.exception.InvalidOrderStatusTransitionException;
import org.example.company.tcs.techcellshop.exception.ResourceNotFoundException;
import org.example.company.tcs.techcellshop.repository.OrderRepository;
import org.example.company.tcs.techcellshop.service.DeviceService;
import org.example.company.tcs.techcellshop.service.impl.order.CancelOrder;
import org.example.company.tcs.techcellshop.util.OrderStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CancelOrderTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private DeviceService deviceService;

    private CancelOrder cancelOrder;

    private Order order;
    private Device device;

    @BeforeEach
    void setUp() {
        cancelOrder = new CancelOrder(orderRepository, deviceService);

        device = new Device();
        device.setIdDevice(1L);

        order = new Order();
        order.setIdOrder(1L);
        order.setDevice(device);
        order.setDeviceIdSnapshot(1L);
        order.setQuantityOrder(2);
        order.setStatus(OrderStatus.CREATED);
    }

    @Nested
    @DisplayName("cancelOrder")
    class CancelOrderFlow {

        @Test
        @DisplayName("should cancel order and release stock")
        void shouldCancelOrderAndReleaseStock() {
            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
            when(orderRepository.save(order)).thenReturn(order);

            Order result = cancelOrder.cancelOrder(1L, "Customer request");

            assertThat(result.getStatus()).isEqualTo(OrderStatus.CANCELED);
            assertThat(result.getCanceledReason()).isEqualTo("Customer request");

            verify(deviceService).releaseStock(1L, 2);
            verify(orderRepository).save(order);
        }

        @Test
        @DisplayName("should cancel order and release stock using device snapshot when relation is null")
        void shouldCancelOrderAndReleaseStockUsingDeviceSnapshotWhenRelationIsNull() {
            order.setDevice(null);
            order.setDeviceIdSnapshot(99L);

            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
            when(orderRepository.save(order)).thenReturn(order);

            Order result = cancelOrder.cancelOrder(1L, "Customer request");

            assertThat(result.getStatus()).isEqualTo(OrderStatus.CANCELED);
            assertThat(result.getCanceledReason()).isEqualTo("Customer request");

            verify(deviceService).releaseStock(99L, 2);
            verify(orderRepository).save(order);
        }

        @Test
        @DisplayName("should use default reason when blank")
        void shouldUseDefaultReasonWhenBlank() {
            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
            when(orderRepository.save(order)).thenReturn(order);

            Order result = cancelOrder.cancelOrder(1L, " ");

            assertThat(result.getStatus()).isEqualTo(OrderStatus.CANCELED);
            assertThat(result.getCanceledReason()).isEqualTo("Order canceled by the user");
        }

        @Test
        @DisplayName("should return same order when already canceled")
        void shouldReturnSameOrderWhenAlreadyCanceled() {
            order.setStatus(OrderStatus.CANCELED);
            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

            Order result = cancelOrder.cancelOrder(1L, "anything");

            assertThat(result).isSameAs(order);

            verify(deviceService, never()).releaseStock(
                    org.mockito.ArgumentMatchers.anyLong(),
                    org.mockito.ArgumentMatchers.anyInt()
            );
            verify(orderRepository, never()).save(order);
        }

        @Test
        @DisplayName("should throw when order is shipped")
        void shouldThrowWhenOrderIsShipped() {
            order.setStatus(OrderStatus.SHIPPED);
            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

            assertThatThrownBy(() -> cancelOrder.cancelOrder(1L, "Customer request"))
                    .isInstanceOf(InvalidOrderStatusTransitionException.class)
                    .hasMessage("Cannot cancel an order that has already been shipped or delivered");
        }

        @Test
        @DisplayName("should throw when order is delivered")
        void shouldThrowWhenOrderIsDelivered() {
            order.setStatus(OrderStatus.DELIVERED);
            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

            assertThatThrownBy(() -> cancelOrder.cancelOrder(1L, "Customer request"))
                    .isInstanceOf(InvalidOrderStatusTransitionException.class)
                    .hasMessage("Cannot cancel an order that has already been shipped or delivered");
        }

        @Test
        @DisplayName("should throw when order does not exist")
        void shouldThrowWhenOrderDoesNotExist() {
            when(orderRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> cancelOrder.cancelOrder(99L, "Customer request"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("No order found with id: 99");
        }
    }
}