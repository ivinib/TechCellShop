package org.example.company.tcs.techcellshop.service.impl;

import jakarta.transaction.Transactional;
import org.example.company.tcs.techcellshop.dto.payment.PaymentActionRequestDto;
import org.example.company.tcs.techcellshop.dto.payment.PaymentResponseDto;
import org.example.company.tcs.techcellshop.domain.Order;
import org.example.company.tcs.techcellshop.exception.ResourceNotFoundException;
import org.example.company.tcs.techcellshop.repository.OrderRepository;
import org.example.company.tcs.techcellshop.service.DeviceService;
import org.example.company.tcs.techcellshop.service.PaymentService;
import org.example.company.tcs.techcellshop.util.OrderStatus;
import org.example.company.tcs.techcellshop.util.PaymentStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Service
public class PaymentServiceImpl implements PaymentService {

    private final OrderRepository orderRepository;
    private final DeviceService deviceService;

    public PaymentServiceImpl(OrderRepository orderRepository, DeviceService deviceService) {
        this.orderRepository = orderRepository;
        this.deviceService = deviceService;
    }

    @Transactional
    @Override
    public PaymentResponseDto confirmPayment(Long orderId, PaymentActionRequestDto request) {
        Order order = getOrderOrThrow(orderId);

        if (order.getStatus().equals(OrderStatus.CANCELED)) {
            throw new IllegalStateException("Cannot confirm payment for a canceled order");
        }

        if (order.getStatus().equals(OrderStatus.SHIPPED) || order.getStatus().equals(OrderStatus.DELIVERED)) {
            throw new IllegalStateException("Cannot confirm payment for an order that is already shipped or delivered");
        }

        if (order.getPaymentStatus().equals(PaymentStatus.REFUNDED)) {
            throw new IllegalStateException("Cannot confirm payment for a refunded order");
        }

        if (order.getPaymentStatus().equals(PaymentStatus.CONFIRMED)) {
            return toResponse(order);
        }

        BigDecimal expectedAmount = order.getFinalAmount() != null
                ? order.getFinalAmount()
                : order.getTotalPriceOrder();

        if (request.getAmount().compareTo(expectedAmount) != 0) {
            throw new IllegalArgumentException("Payment amount does not match the order total");
        }

        order.setPaymentStatus(PaymentStatus.CONFIRMED);
        order.setStatus(OrderStatus.PAID);

        orderRepository.save(order);
        return toResponse(order);
    }

    @Transactional
    @Override
    public PaymentResponseDto failPayment(Long orderId, PaymentActionRequestDto request) {
        Order order = getOrderOrThrow(orderId);

        if (order.getPaymentStatus().equals(PaymentStatus.FAILED)) {
            return toResponse(order);
        }

        if (order.getPaymentStatus().equals(PaymentStatus.CONFIRMED)) {
            throw new IllegalStateException("Cannot fail payment that is already confirmed");
        }

        if (order.getPaymentStatus().equals(PaymentStatus.REFUNDED)) {
            throw new IllegalStateException("Cannot fail payment that is already refunded");
        }

        if (order.getStatus().equals(OrderStatus.SHIPPED) || order.getStatus().equals(OrderStatus.DELIVERED)) {
            throw new IllegalStateException("Cannot fail payment for an order that is already shipped or delivered");
        }

        if (!order.getStatus().equals(OrderStatus.CANCELED)) {
            deviceService.releaseStock(resolveDeviceId(order), order.getQuantityOrder());
        }

        order.setPaymentStatus(PaymentStatus.FAILED);
        order.setStatus(OrderStatus.CANCELED);
        order.setCanceledReason(
                request.getReason() == null || request.getReason().isBlank()
                        ? "Payment confirmation failed"
                        : request.getReason()
        );

        orderRepository.save(order);
        return toResponse(order);
    }

    @Transactional
    @Override
    public PaymentResponseDto refundPayment(Long orderId, PaymentActionRequestDto request) {
        Order order = getOrderOrThrow(orderId);

        if (order.getPaymentStatus().equals(PaymentStatus.REFUNDED)) {
            return toResponse(order);
        }

        if (!order.getStatus().equals(OrderStatus.CANCELED)) {
            throw new IllegalStateException("Cannot refund payment for an order that is not canceled");
        }

        if (!order.getPaymentStatus().equals(PaymentStatus.CONFIRMED)) {
            throw new IllegalStateException("Refund is only allowed for order with confirmed payment");
        }

        order.setPaymentStatus(PaymentStatus.REFUNDED);

        orderRepository.save(order);
        return toResponse(order);
    }

    private Order getOrderOrThrow(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));
    }

    private Long resolveDeviceId(Order order) {
        if (order.getDevice() != null && order.getDevice().getIdDevice() != null) {
            return order.getDevice().getIdDevice();
        }
        return order.getDeviceIdSnapshot();
    }

    private PaymentResponseDto toResponse(Order order) {
        PaymentResponseDto response = new PaymentResponseDto();
        response.setOrderId(order.getIdOrder());
        response.setPaymentStatus(order.getPaymentStatus());
        response.setProcessedAt(OffsetDateTime.now());
        return response;
    }
}