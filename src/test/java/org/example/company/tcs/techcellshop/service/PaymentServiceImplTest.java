package org.example.company.tcs.techcellshop.service;

import org.example.company.tcs.techcellshop.domain.Device;
import org.example.company.tcs.techcellshop.domain.Order;
import org.example.company.tcs.techcellshop.dto.payment.PaymentActionRequestDto;
import org.example.company.tcs.techcellshop.dto.payment.PaymentResponseDto;
import org.example.company.tcs.techcellshop.exception.ResourceNotFoundException;
import org.example.company.tcs.techcellshop.repository.OrderRepository;
import org.example.company.tcs.techcellshop.service.impl.PaymentServiceImpl;
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

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private DeviceService deviceService;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    private Order order;
    private Device device;
    private PaymentActionRequestDto request;

    @BeforeEach
    void setUp() {
        device = new Device();
        device.setIdDevice(1L);
        device.setNameDevice("Galaxy S24");
        device.setDeviceStock(10);

        order = new Order();
        order.setIdOrder(1L);
        order.setDevice(device);
        order.setQuantityOrder(2);
        order.setTotalPriceOrder(money("7999.80"));
        order.setFinalAmount(money("7899.80"));
        order.setStatus(OrderStatus.CREATED);
        order.setPaymentStatus(PaymentStatus.PENDING);

        request = new PaymentActionRequestDto();
        request.setTransactionId("TXN-1001");
        request.setAmount(money("7899.80"));
        request.setReason("Customer requested operation");
    }

    @Nested
    @DisplayName("confirmPayment")
    class ConfirmPayment {

        @Test
        @DisplayName("should confirm payment when amount matches final amount")
        void shouldConfirmPaymentWhenAmountMatchesFinalAmount() {
            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
            when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

            PaymentResponseDto result = paymentService.confirmPayment(1L, request);

            assertThat(result.getOrderId()).isEqualTo(1L);
            assertThat(result.getPaymentStatus()).isEqualTo(PaymentStatus.CONFIRMED);

            assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.CONFIRMED);
            assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);

            verify(orderRepository).save(order);
        }

        @Test
        @DisplayName("should confirm payment using total price when final amount is null")
        void shouldConfirmPaymentUsingTotalPriceWhenFinalAmountIsNull() {
            order.setFinalAmount(null);
            request.setAmount(money("7999.80"));

            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
            when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

            PaymentResponseDto result = paymentService.confirmPayment(1L, request);

            assertThat(result.getPaymentStatus()).isEqualTo(PaymentStatus.CONFIRMED);
            assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.CONFIRMED);
            assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);

            verify(orderRepository).save(order);
        }

        @Test
        @DisplayName("should return existing response when payment is already confirmed")
        void shouldReturnExistingResponseWhenPaymentIsAlreadyConfirmed() {
            order.setPaymentStatus(PaymentStatus.CONFIRMED);

            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

            PaymentResponseDto result = paymentService.confirmPayment(1L, request);

            assertThat(result.getOrderId()).isEqualTo(1L);
            assertThat(result.getPaymentStatus()).isEqualTo(PaymentStatus.CONFIRMED);

            verify(orderRepository, never()).save(any(Order.class));
        }

        @Test
        @DisplayName("should throw when order is canceled")
        void shouldThrowWhenOrderIsCanceled() {
            order.setStatus(OrderStatus.CANCELED);

            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

            assertThatThrownBy(() -> paymentService.confirmPayment(1L, request))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Cannot confirm payment for a canceled order");

            verify(orderRepository, never()).save(any(Order.class));
        }

        @Test
        @DisplayName("should throw when order is shipped")
        void shouldThrowWhenOrderIsShipped() {
            order.setStatus(OrderStatus.SHIPPED);

            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

            assertThatThrownBy(() -> paymentService.confirmPayment(1L, request))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Cannot confirm payment for an order that is already shipped or delivered");

            verify(orderRepository, never()).save(any(Order.class));
        }

        @Test
        @DisplayName("should throw when order is delivered")
        void shouldThrowWhenOrderIsDelivered() {
            order.setStatus(OrderStatus.DELIVERED);

            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

            assertThatThrownBy(() -> paymentService.confirmPayment(1L, request))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Cannot confirm payment for an order that is already shipped or delivered");

            verify(orderRepository, never()).save(any(Order.class));
        }

        @Test
        @DisplayName("should throw when payment is already refunded")
        void shouldThrowWhenPaymentIsAlreadyRefunded() {
            order.setPaymentStatus(PaymentStatus.REFUNDED);

            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

            assertThatThrownBy(() -> paymentService.confirmPayment(1L, request))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Cannot confirm payment for a refunded order");

            verify(orderRepository, never()).save(any(Order.class));
        }

        @Test
        @DisplayName("should throw when payment amount does not match order total")
        void shouldThrowWhenPaymentAmountDoesNotMatchOrderTotal() {
            request.setAmount(money("7000.00"));

            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

            assertThatThrownBy(() -> paymentService.confirmPayment(1L, request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Payment amount does not match the order total");

            verify(orderRepository, never()).save(any(Order.class));
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when order does not exist")
        void shouldThrowWhenOrderDoesNotExist() {
            when(orderRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> paymentService.confirmPayment(99L, request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Order not found with id: 99");

            verify(orderRepository, never()).save(any(Order.class));
        }
    }

    @Nested
    @DisplayName("failPayment")
    class FailPayment {

        @Test
        @DisplayName("should fail payment cancel order and release stock")
        void shouldFailPaymentCancelOrderAndReleaseStock() {
            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
            when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

            PaymentResponseDto result = paymentService.failPayment(1L, request);

            assertThat(result.getOrderId()).isEqualTo(1L);
            assertThat(result.getPaymentStatus()).isEqualTo(PaymentStatus.FAILED);

            assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.FAILED);
            assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELED);
            assertThat(order.getCanceledReason()).isEqualTo("Customer requested operation");

            verify(deviceService).releaseStock(1L, 2);
            verify(orderRepository).save(order);
        }

        @Test
        @DisplayName("should use default cancel reason when reason is blank")
        void shouldUseDefaultCancelReasonWhenReasonIsBlank() {
            request.setReason("   ");

            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
            when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

            PaymentResponseDto result = paymentService.failPayment(1L, request);

            assertThat(result.getPaymentStatus()).isEqualTo(PaymentStatus.FAILED);
            assertThat(order.getCanceledReason()).isEqualTo("Payment confirmation failed");

            verify(deviceService).releaseStock(1L, 2);
            verify(orderRepository).save(order);
        }

        @Test
        @DisplayName("should return existing response when payment is already failed")
        void shouldReturnExistingResponseWhenPaymentIsAlreadyFailed() {
            order.setPaymentStatus(PaymentStatus.FAILED);

            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

            PaymentResponseDto result = paymentService.failPayment(1L, request);

            assertThat(result.getOrderId()).isEqualTo(1L);
            assertThat(result.getPaymentStatus()).isEqualTo(PaymentStatus.FAILED);

            verify(deviceService, never()).releaseStock(any(), any());
            verify(orderRepository, never()).save(any(Order.class));
        }

        @Test
        @DisplayName("should throw when payment is already confirmed")
        void shouldThrowWhenPaymentIsAlreadyConfirmed() {
            order.setPaymentStatus(PaymentStatus.CONFIRMED);

            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

            assertThatThrownBy(() -> paymentService.failPayment(1L, request))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Cannot fail payment that is already confirmed");

            verify(deviceService, never()).releaseStock(any(), any());
            verify(orderRepository, never()).save(any(Order.class));
        }

        @Test
        @DisplayName("should throw when payment is already refunded")
        void shouldThrowWhenPaymentIsAlreadyRefunded() {
            order.setPaymentStatus(PaymentStatus.REFUNDED);

            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

            assertThatThrownBy(() -> paymentService.failPayment(1L, request))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Cannot fail payment that is already refunded");

            verify(deviceService, never()).releaseStock(any(), any());
            verify(orderRepository, never()).save(any(Order.class));
        }

        @Test
        @DisplayName("should not release stock when order is already canceled")
        void shouldNotReleaseStockWhenOrderIsAlreadyCanceled() {
            order.setStatus(OrderStatus.CANCELED);

            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
            when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

            PaymentResponseDto result = paymentService.failPayment(1L, request);

            assertThat(result.getPaymentStatus()).isEqualTo(PaymentStatus.FAILED);
            assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELED);

            verify(deviceService, never()).releaseStock(any(), any());
            verify(orderRepository).save(order);
        }

        @Test
        @DisplayName("should throw when order is shipped")
        void shouldThrowWhenOrderIsShipped() {
            order.setStatus(OrderStatus.SHIPPED);

            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

            assertThatThrownBy(() -> paymentService.failPayment(1L, request))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Cannot fail payment for an order that is already shipped or delivered");

            verify(deviceService, never()).releaseStock(any(), any());
            verify(orderRepository, never()).save(any(Order.class));
        }

        @Test
        @DisplayName("should throw when order is delivered")
        void shouldThrowWhenOrderIsDelivered() {
            order.setStatus(OrderStatus.DELIVERED);

            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

            assertThatThrownBy(() -> paymentService.failPayment(1L, request))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Cannot fail payment for an order that is already shipped or delivered");

            verify(deviceService, never()).releaseStock(any(), any());
            verify(orderRepository, never()).save(any(Order.class));
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when order does not exist")
        void shouldThrowWhenOrderDoesNotExist() {
            when(orderRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> paymentService.failPayment(99L, request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Order not found with id: 99");

            verify(deviceService, never()).releaseStock(any(), any());
            verify(orderRepository, never()).save(any(Order.class));
        }
    }

    @Nested
    @DisplayName("refundPayment")
    class RefundPayment {

        @Test
        @DisplayName("should refund payment when order is canceled and payment is confirmed")
        void shouldRefundPaymentWhenOrderIsCanceledAndPaymentIsConfirmed() {
            order.setStatus(OrderStatus.CANCELED);
            order.setPaymentStatus(PaymentStatus.CONFIRMED);

            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
            when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

            PaymentResponseDto result = paymentService.refundPayment(1L, request);

            assertThat(result.getOrderId()).isEqualTo(1L);
            assertThat(result.getPaymentStatus()).isEqualTo(PaymentStatus.REFUNDED);
            assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.REFUNDED);

            verify(orderRepository).save(order);
        }

        @Test
        @DisplayName("should return existing response when payment is already refunded")
        void shouldReturnExistingResponseWhenPaymentIsAlreadyRefunded() {
            order.setPaymentStatus(PaymentStatus.REFUNDED);

            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

            PaymentResponseDto result = paymentService.refundPayment(1L, request);

            assertThat(result.getOrderId()).isEqualTo(1L);
            assertThat(result.getPaymentStatus()).isEqualTo(PaymentStatus.REFUNDED);

            verify(orderRepository, never()).save(any(Order.class));
        }

        @Test
        @DisplayName("should throw when order is not canceled")
        void shouldThrowWhenOrderIsNotCanceled() {
            order.setStatus(OrderStatus.PAID);
            order.setPaymentStatus(PaymentStatus.CONFIRMED);

            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

            assertThatThrownBy(() -> paymentService.refundPayment(1L, request))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Cannot refund payment for an order that is not canceled");

            verify(orderRepository, never()).save(any(Order.class));
        }

        @Test
        @DisplayName("should throw when payment is not confirmed")
        void shouldThrowWhenPaymentIsNotConfirmed() {
            order.setStatus(OrderStatus.CANCELED);
            order.setPaymentStatus(PaymentStatus.FAILED);

            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

            assertThatThrownBy(() -> paymentService.refundPayment(1L, request))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Refund is only allowed for order with confirmed payment");

            verify(orderRepository, never()).save(any(Order.class));
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when order does not exist")
        void shouldThrowWhenOrderDoesNotExist() {
            when(orderRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> paymentService.refundPayment(99L, request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Order not found with id: 99");

            verify(orderRepository, never()).save(any(Order.class));
        }
    }

    private BigDecimal money(String value) {
        return new BigDecimal(value);
    }
}