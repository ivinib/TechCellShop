package org.example.company.tcs.techcellshop.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.example.company.tcs.techcellshop.domain.ErrorResponse;
import org.example.company.tcs.techcellshop.dto.payment.PaymentActionRequestDto;
import org.example.company.tcs.techcellshop.dto.payment.PaymentResponseDto;
import org.example.company.tcs.techcellshop.service.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static org.example.company.tcs.techcellshop.util.AppConstants.SECURITY_SCHEME_NAME;

@Tag(name = "Payment Management", description = "Endpoints for managing payment actions such as confirmation, failure, and refunds")
@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @Operation(
            summary = "Confirm payment",
            description = "Confirms payment for an existing order when the order state and amount are valid",
            security = @SecurityRequirement(name = SECURITY_SCHEME_NAME)
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Payment confirmed"),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid payload or amount mismatch",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(ref = "#/components/examples/ValidationErrorExample"),
                                    @ExampleObject(ref = "#/components/examples/InvalidArgumentExample")
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(ref = "#/components/examples/ForbiddenErrorExample")
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
                    description = "Order/payment state conflict",
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
                            examples = @ExampleObject(ref = "#/components/examples/UnauthorizedErrorExample")
                    )
            )
    })
    @PostMapping("/orders/{orderId}/confirm")
    public ResponseEntity<PaymentResponseDto> confirm(
            @PathVariable Long orderId,
            @Valid
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Payment confirmation payload",
                    required = true,
                    content = @Content(
                            examples = @ExampleObject(
                                    name = "Confirm payment example",
                                    value = """
                                            {
                                              "transactionId": "TXN-1001",
                                              "amount": 2899.90,
                                              "reason": "Payment approved"
                                            }
                                            """
                            )
                    )
            )
            @RequestBody PaymentActionRequestDto request
    ) {
        return ResponseEntity.ok(paymentService.confirmPayment(orderId, request));
    }

    @Operation(
            summary = "Fail payment",
            description = "Marks payment as failed and cancels the order when business rules allow it",
            security = @SecurityRequirement(name = SECURITY_SCHEME_NAME)
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Payment failed and order canceled"),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request payload",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(ref = "#/components/examples/ValidationErrorExample")
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(ref = "#/components/examples/ForbiddenErrorExample")
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
                    description = "Order/payment state conflict",
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
                            examples = @ExampleObject(ref = "#/components/examples/UnauthorizedErrorExample")
                    )
            )
    })
    @PostMapping("/orders/{orderId}/fail")
    public ResponseEntity<PaymentResponseDto> fail(
            @PathVariable Long orderId,
            @Valid
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Payment failure payload",
                    required = true,
                    content = @Content(
                            examples = @ExampleObject(
                                    name = "Fail payment example",
                                    value = """
                                            {
                                              "transactionId": "TXN-1002",
                                              "amount": 2899.90,
                                              "reason": "Card denied"
                                            }
                                            """
                            )
                    )
            )
            @RequestBody PaymentActionRequestDto request
    ) {
        return ResponseEntity.ok(paymentService.failPayment(orderId, request));
    }

    @Operation(
            summary = "Refund payment",
            description = "Refunds a confirmed payment for an order that has already been canceled",
            security = @SecurityRequirement(name = SECURITY_SCHEME_NAME)
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Payment refunded"),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request payload",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(ref = "#/components/examples/ValidationErrorExample")
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(ref = "#/components/examples/ForbiddenErrorExample")
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
                    description = "Refund not allowed for the current order/payment state",
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
                            examples = @ExampleObject(ref = "#/components/examples/UnauthorizedErrorExample")
                    )
            )
    })
    @PostMapping("/orders/{orderId}/refund")
    public ResponseEntity<PaymentResponseDto> refund(
            @PathVariable Long orderId,
            @Valid
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Refund payload",
                    required = true,
                    content = @Content(
                            examples = @ExampleObject(
                                    name = "Refund payment example",
                                    value = """
                                            {
                                              "transactionId": "TXN-1003",
                                              "amount": 2899.90,
                                              "reason": "Customer refunded"
                                            }
                                            """
                            )
                    )
            )
            @RequestBody PaymentActionRequestDto request
    ) {
        return ResponseEntity.ok(paymentService.refundPayment(orderId, request));
    }
}