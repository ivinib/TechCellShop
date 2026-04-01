package org.example.company.tcs.techcellshop.service;

import org.example.company.tcs.techcellshop.controller.dto.payment.PaymentActionRequestDto;
import org.example.company.tcs.techcellshop.controller.dto.payment.PaymentResponseDto;

public interface PaymentService {
    PaymentResponseDto confirmPayment(Long orderId, PaymentActionRequestDto request);
    PaymentResponseDto failPayment(Long orderId, PaymentActionRequestDto request);
    PaymentResponseDto refundPayment(Long orderId, PaymentActionRequestDto request);
}
