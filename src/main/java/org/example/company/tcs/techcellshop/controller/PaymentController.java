package org.example.company.tcs.techcellshop.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.example.company.tcs.techcellshop.dto.payment.PaymentActionRequestDto;
import org.example.company.tcs.techcellshop.dto.payment.PaymentResponseDto;
import org.example.company.tcs.techcellshop.domain.ErrorResponse;
import org.example.company.tcs.techcellshop.service.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Payment Managment", description = "Endpoints for managing payment actions such as confirmation, failure, and refunds")
@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @Operation(summary = "Confirm payment", description = "Confirms payment for an existing order")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Payment operation completed"),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request payload",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(ref = "#/components/examples/ValidationErrorExample")
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Order not found",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(ref = "#/components/examples/NotFoundErrorExample")
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Invalid payment transition",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(ref = "#/components/examples/BusinessConflictExample")
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(ref = "#/components/examples/InvalidArgumentExample")
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Unexpected internal error",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(ref = "#/components/examples/InternalErrorExample")
                    )
            )
    })
    @PostMapping("/orders/{orderId}/confirm")
    public ResponseEntity<PaymentResponseDto> confirm(@PathVariable Long orderId, @Valid @RequestBody PaymentActionRequestDto request) {
        return ResponseEntity.ok(paymentService.confirmPayment(orderId, request));
    }

    @Operation(summary = "Fail payment", description = "Marks payment as failed for an existing order")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Payment operation completed"),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request payload",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(ref = "#/components/examples/ValidationErrorExample")
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Order not found",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(ref = "#/components/examples/NotFoundErrorExample")
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Invalid payment transition",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(ref = "#/components/examples/BusinessConflictExample")
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(ref = "#/components/examples/InvalidArgumentExample")
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Unexpected internal error",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(ref = "#/components/examples/InternalErrorExample")
                    )
            )
    })
    @PostMapping("/orders/{orderId}/fail")
    public ResponseEntity<PaymentResponseDto> fail(@PathVariable Long orderId, @Valid @RequestBody PaymentActionRequestDto request) {
        return ResponseEntity.ok(paymentService.failPayment(orderId, request));
    }

    @Operation(summary = "Refund payment", description = "Refunds a previously confirmed payment")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Payment operation completed"),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request payload",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(ref = "#/components/examples/ValidationErrorExample")
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Order not found",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(ref = "#/components/examples/NotFoundErrorExample")
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Invalid payment transition",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(ref = "#/components/examples/BusinessConflictExample")
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(ref = "#/components/examples/InvalidArgumentExample")
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Unexpected internal error",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(ref = "#/components/examples/InternalErrorExample")
                    )
            )
    })
    @PostMapping("/orders/{orderId}/refund")
    public ResponseEntity<PaymentResponseDto> refund(@PathVariable Long orderId, @Valid @RequestBody PaymentActionRequestDto request) {
        return ResponseEntity.ok(paymentService.refundPayment(orderId, request));
    }
}
