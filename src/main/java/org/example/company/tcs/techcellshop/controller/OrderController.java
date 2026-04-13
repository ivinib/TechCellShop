package org.example.company.tcs.techcellshop.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.example.company.tcs.techcellshop.domain.ErrorResponse;
import org.example.company.tcs.techcellshop.domain.Order;
import org.example.company.tcs.techcellshop.dto.order.OrderStatusUpdateRequestDto;
import org.example.company.tcs.techcellshop.dto.request.OrderEnrollmentRequest;
import org.example.company.tcs.techcellshop.dto.request.OrderUpdateRequest;
import org.example.company.tcs.techcellshop.dto.response.OrderResponse;
import org.example.company.tcs.techcellshop.mapper.ResponseMapper;
import org.example.company.tcs.techcellshop.service.OrderService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

import static org.example.company.tcs.techcellshop.util.AppConstants.SECURITY_SCHEME_NAME;

@Tag(name = "Order Management", description = "Order management endpoints")
@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;
    private final ResponseMapper responseMapper;

    OrderController(OrderService orderService, ResponseMapper responseMapper) {
        this.orderService = orderService;
        this.responseMapper = responseMapper;
    }

    @Operation(
            summary = "Place a new order",
            description = "Creates an order for the authenticated user, requires the Idempotency-Key header and triggers the order placement flow",
            security = @SecurityRequirement(name = SECURITY_SCHEME_NAME)
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Order created"),
            @ApiResponse(
                    responseCode = "400",
                    description = "Validation error or missing Idempotency-Key",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(ref = "#/components/examples/ValidationErrorExample"),
                                    @ExampleObject(ref = "#/components/examples/InvalidArgumentExample")
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(ref = "#/components/examples/UnauthorizedErrorExample")
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Business conflict such as insufficient stock or reused idempotency key for different payload",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(ref = "#/components/examples/BusinessConflictExample")
                    )
            )
    })
    @PostMapping
    public ResponseEntity<OrderResponse> saveOrder(
            @Parameter(description = "Unique idempotency key for safe retries", required = true, example = "ORDER-KEY-001")
            @RequestHeader("Idempotency-Key") @NotBlank String idempotencyKey,
            @Valid
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Order enrollment payload",
                    required = true,
                    content = @Content(
                            examples = @ExampleObject(
                                    name = "Order payload example",
                                    value = """
                                            {
                                              "idDevice": 1,
                                              "quantityOrder": 1,
                                              "paymentMethod": "PIX"
                                            }
                                            """
                            )
                    )
            )
            @RequestBody OrderEnrollmentRequest request,
            Authentication authentication,
            UriComponentsBuilder uriBuilder
    ) {
        Order savedOrder = orderService.placeOrder(request, authentication.getName(), idempotencyKey);
        OrderResponse response = responseMapper.toOrderResponse(savedOrder);

        URI location = uriBuilder
                .path("/api/v1/orders/{id}")
                .buildAndExpand(savedOrder.getIdOrder())
                .toUri();

        return ResponseEntity.created(location).body(response);
    }

    @Operation(
            summary = "List all orders",
            description = "Returns a paginated list of all orders. Restricted to admin users.",
            security = @SecurityRequirement(name = SECURITY_SCHEME_NAME)
    )
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<Page<OrderResponse>> getAllOrders(
            @PageableDefault(size = 20, sort = "idOrder") Pageable pageable
    ) {
        Page<Order> orders = orderService.getAllOrders(pageable);
        Page<OrderResponse> response = orders.map(responseMapper::toOrderResponse);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "List authenticated user's orders",
            description = "Returns a paginated list of orders that belong to the authenticated user.",
            security = @SecurityRequirement(name = SECURITY_SCHEME_NAME)
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Orders returned"),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(ref = "#/components/examples/UnauthorizedErrorExample")
                    )
            )
    })
    @GetMapping("/me")
    public ResponseEntity<Page<OrderResponse>> getMyOrders(
            Authentication authentication,
            @PageableDefault(size = 20, sort = "idOrder") Pageable pageable
    ) {
        Page<Order> orders = orderService.getOrdersForUser(authentication.getName(), pageable);
        Page<OrderResponse> response = orders.map(responseMapper::toOrderResponse);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Get order by id",
            description = "Returns a specific order by id. Admin can access any order; regular users can only access their own orders.",
            security = @SecurityRequirement(name = SECURITY_SCHEME_NAME)
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Order returned"),
            @ApiResponse(
                    responseCode = "404",
                    description = "Order not found",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(ref = "#/components/examples/NotFoundErrorExample")
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(ref = "#/components/examples/UnauthorizedErrorExample")
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(ref = "#/components/examples/ForbiddenErrorExample")
                    )
            )
    })
    @PreAuthorize("hasRole('ADMIN') or @accessPolicy.canAccessOrder(#id, authentication)")
    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrderById(@PathVariable Long id) {
        Order order = orderService.getOrderById(id);
        return ResponseEntity.ok(responseMapper.toOrderResponse(order));
    }

    @Operation(
            summary = "Update order",
            description = "Updates an order using a full payload. Restricted to admin users.",
            security = @SecurityRequirement(name = SECURITY_SCHEME_NAME)
    )
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<OrderResponse> updateOrder(@PathVariable Long id, @Valid @RequestBody OrderUpdateRequest request) {
        Order updatedOrder = orderService.updateOrder(id, request);
        return ResponseEntity.ok(responseMapper.toOrderResponse(updatedOrder));
    }

    @Operation(
            summary = "Partially update order",
            description = "Updates selected order fields. Restricted to admin users.",
            security = @SecurityRequirement(name = SECURITY_SCHEME_NAME)
    )
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}")
    public ResponseEntity<OrderResponse> partiallyUpdateOrder(@PathVariable Long id, @Valid @RequestBody OrderUpdateRequest request) {
        Order updatedOrder = orderService.updateOrder(id, request);
        return ResponseEntity.ok(responseMapper.toOrderResponse(updatedOrder));
    }

    @Operation(
            summary = "Delete order",
            description = "Deletes an order by id. Restricted to admin users.",
            security = @SecurityRequirement(name = SECURITY_SCHEME_NAME)
    )
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOrder(@PathVariable Long id) {
        orderService.deleteOrder(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Update order status",
            description = "Changes the order status according to business transition rules. Restricted to admin users.",
            security = @SecurityRequirement(name = SECURITY_SCHEME_NAME)
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Order status updated"),
            @ApiResponse(
                    responseCode = "400",
                    description = "Validation error",
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
                    description = "Invalid status transition or payment/order state conflict",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(ref = "#/components/examples/BusinessConflictExample")
                    )
            )
    })
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/status")
    public ResponseEntity<OrderResponse> updateStatus(
            @PathVariable Long id,
            @Valid
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "New status payload",
                    required = true,
                    content = @Content(
                            examples = @ExampleObject(
                                    name = "Status update example",
                                    value = """
                                            {
                                              "newStatus": "SHIPPED",
                                              "reason": "Sent to carrier"
                                            }
                                            """
                            )
                    )
            )
            @RequestBody OrderStatusUpdateRequestDto request
    ) {
        Order updated = orderService.updateStatus(id, request.getNewStatus(), request.getReason());
        return ResponseEntity.ok(responseMapper.toOrderResponse(updated));
    }

    @Operation(
            summary = "Cancel order",
            description = "Cancels an order when business rules allow it. Admin can cancel any order; regular users can cancel their own orders.",
            security = @SecurityRequirement(name = SECURITY_SCHEME_NAME)
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Order canceled"),
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
                    description = "Order cannot be canceled in the current state",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(ref = "#/components/examples/BusinessConflictExample")
                    )
            )
    })
    @PreAuthorize("hasRole('ADMIN') or @accessPolicy.canAccessOrder(#id, authentication)")
    @PostMapping("/{id}/cancel")
    public ResponseEntity<OrderResponse> cancelOrder(
            @PathVariable Long id,
            @RequestParam(required = false) String reason
    ) {
        Order canceled = orderService.cancelOrder(id, reason);
        return ResponseEntity.ok(responseMapper.toOrderResponse(canceled));
    }

    @Operation(
            summary = "Apply coupon to order",
            description = "Applies a coupon to an order when the order state and coupon rules allow it.",
            security = @SecurityRequirement(name = SECURITY_SCHEME_NAME)
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Coupon applied"),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid coupon request",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(ref = "#/components/examples/ValidationErrorExample"),
                                    @ExampleObject(ref = "#/components/examples/InvalidArgumentExample")
                            }
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
                    description = "Coupon or order state conflict",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(ref = "#/components/examples/BusinessConflictExample")
                    )
            )
    })
    @PreAuthorize("hasRole('ADMIN') or @accessPolicy.canAccessOrder(#id, authentication)")
    @PostMapping("/{id}/apply-coupon")
    public ResponseEntity<OrderResponse> applyCoupon(
            @PathVariable Long id,
            @RequestParam @NotBlank String code
    ) {
        Order updated = orderService.applyCoupon(id, code);
        return ResponseEntity.ok(responseMapper.toOrderResponse(updated));
    }
}