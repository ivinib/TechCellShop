package org.example.company.tcs.techcellshop.controller;

import jakarta.validation.Valid;
import org.example.company.tcs.techcellshop.controller.dto.payment.PaymentActionRequestDto;
import org.example.company.tcs.techcellshop.controller.dto.payment.PaymentResponseDto;
import org.example.company.tcs.techcellshop.service.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/orders/{orderId}/confirm")
    public ResponseEntity<PaymentResponseDto> confirm(
            @PathVariable Long orderId,
            @Valid @RequestBody PaymentActionRequestDto request) {
        return ResponseEntity.ok(paymentService.confirmPayment(orderId, request));
    }

    @PostMapping("/orders/{orderId}/fail")
    public ResponseEntity<PaymentResponseDto> fail(
            @PathVariable Long orderId,
            @Valid @RequestBody PaymentActionRequestDto request) {
        return ResponseEntity.ok(paymentService.failPayment(orderId, request));
    }

    @PostMapping("/orders/{orderId}/refund")
    public ResponseEntity<PaymentResponseDto> refund(
            @PathVariable Long orderId,
            @Valid @RequestBody PaymentActionRequestDto request) {
        return ResponseEntity.ok(paymentService.refundPayment(orderId, request));
    }
}
