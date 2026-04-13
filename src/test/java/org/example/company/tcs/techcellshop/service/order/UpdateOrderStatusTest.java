package org.example.company.tcs.techcellshop.service.order;

import org.example.company.tcs.techcellshop.domain.Order;
import org.example.company.tcs.techcellshop.exception.InvalidOrderStatusTransitionException;
import org.example.company.tcs.techcellshop.exception.ResourceNotFoundException;
import org.example.company.tcs.techcellshop.repository.OrderRepository;
import org.example.company.tcs.techcellshop.service.OrderStatusTransitionValidator;
import org.example.company.tcs.techcellshop.service.impl.order.CancelOrder;
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

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UpdateOrderStatusTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderStatusTransitionValidator orderStatusTransitionValidator;

    @Mock
    private CancelOrder cancelOrder;

    private UpdateOrderStatus updateOrderStatus;

    private Order order;

    @BeforeEach
    void setUp() {
        updateOrderStatus = new UpdateOrderStatus(
                orderRepository,
                orderStatusTransitionValidator,
                cancelOrder
        );

        order = new Order();
        order.setIdOrder(1L);
        order.setStatus(OrderStatus.CREATED);
        order.setPaymentStatus(PaymentStatus.PENDING);
    }

    @Nested
    @DisplayName("updateStatus")
    class UpdateStatus {

        @Test
        @DisplayName("should update status to PAID when payment is confirmed")
        void shouldUpdateStatusToPaidWhenPaymentIsConfirmed() {
            order.setPaymentStatus(PaymentStatus.CONFIRMED);

            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
            when(orderRepository.save(order)).thenReturn(order);

            Order result = updateOrderStatus.updateStatus(1L, OrderStatus.PAID, null);

            assertThat(result.getStatus()).isEqualTo(OrderStatus.PAID);

            verify(orderStatusTransitionValidator).validateTransition(OrderStatus.CREATED, OrderStatus.PAID);
            verify(orderRepository).save(order);
        }

        @Test
        @DisplayName("should throw when marking order as paid without confirmed payment")
        void shouldThrowWhenMarkingOrderAsPaidWithoutConfirmedPayment() {
            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

            assertThatThrownBy(() -> updateOrderStatus.updateStatus(1L, OrderStatus.PAID, null))
                    .isInstanceOf(InvalidOrderStatusTransitionException.class)
                    .hasMessage("Order cannot be marked as paid without confirmed payment");
        }

        @Test
        @DisplayName("should throw when shipping without confirmed payment")
        void shouldThrowWhenShippingWithoutConfirmedPayment() {
            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

            assertThatThrownBy(() -> updateOrderStatus.updateStatus(1L, OrderStatus.SHIPPED, null))
                    .isInstanceOf(InvalidOrderStatusTransitionException.class)
                    .hasMessage("Order cannot be shipped without confirmed payment");
        }

        @Test
        @DisplayName("should allow shipping with confirmed payment")
        void shouldAllowShippingWithConfirmedPayment() {
            order.setPaymentStatus(PaymentStatus.CONFIRMED);
            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
            when(orderRepository.save(order)).thenReturn(order);

            Order result = updateOrderStatus.updateStatus(1L, OrderStatus.SHIPPED, null);

            assertThat(result.getStatus()).isEqualTo(OrderStatus.SHIPPED);

            verify(orderStatusTransitionValidator).validateTransition(OrderStatus.CREATED, OrderStatus.SHIPPED);
            verify(orderRepository).save(order);
        }

        @Test
        @DisplayName("should delegate to cancel use case when new status is canceled")
        void shouldDelegateToCancelUseCaseWhenNewStatusIsCanceled() {
            Order canceled = new Order();
            canceled.setIdOrder(1L);
            canceled.setStatus(OrderStatus.CANCELED);

            when(cancelOrder.cancelOrder(1L, "Customer request")).thenReturn(canceled);

            Order result = updateOrderStatus.updateStatus(1L, OrderStatus.CANCELED, "Customer request");

            assertThat(result.getStatus()).isEqualTo(OrderStatus.CANCELED);

            verify(cancelOrder).cancelOrder(1L, "Customer request");
            verify(orderRepository, never()).findById(1L);
        }

        @Test
        @DisplayName("should throw when order does not exist")
        void shouldThrowWhenOrderDoesNotExist() {
            when(orderRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> updateOrderStatus.updateStatus(99L, OrderStatus.PAID, null))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("No order found with id: 99");
        }
    }
}
