package org.example.company.tcs.techcellshop.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.example.company.tcs.techcellshop.controller.dto.order.OrderStatusUpdateRequestDto;
import org.example.company.tcs.techcellshop.controller.dto.request.OrderEnrollmentRequest;
import org.example.company.tcs.techcellshop.controller.dto.request.OrderUpdateRequest;
import org.example.company.tcs.techcellshop.controller.dto.response.OrderResponse;
import org.example.company.tcs.techcellshop.domain.Order;
import org.example.company.tcs.techcellshop.mapper.RequestMapper;
import org.example.company.tcs.techcellshop.mapper.ResponseMapper;
import org.example.company.tcs.techcellshop.service.OrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

import static org.example.company.tcs.techcellshop.util.AppConstants.SECURITY_SCHEME_NAME;

@Tag(name = "Orders managment", description = "Order management endpoints")
@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;
    private final RequestMapper requestMapper;
    private final ResponseMapper responseMapper;

    OrderController(OrderService orderService, RequestMapper requestMapper, ResponseMapper responseMapper) {
        this.orderService = orderService;
        this.requestMapper = requestMapper;
        this.responseMapper = responseMapper;
    }

    @Operation(
            summary = "Place a new order",
            description = "Creates an order and triggers the order placement flow",
            security = @SecurityRequirement(name = SECURITY_SCHEME_NAME)
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Order created"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PostMapping
    public ResponseEntity<OrderResponse> saveOrder(
            @Valid
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Order enrollment payload",
                    required = true,
                    content = @Content(
                            examples = @ExampleObject(
                                    name = "Order payload example",
                                    value = """
                                            {
                                              "idUser": 1,
                                              "idDevice": 1,
                                              "quantityOrder": 1,
                                              "addressOrder": "Sao Paulo - SP",
                                              "paymentMethodOrder": "CREDIT_CARD"
                                            }
                                            """
                            )
                    )
            )
            @RequestBody OrderEnrollmentRequest request,
            UriComponentsBuilder uriBuilder) {
        Order savedOrder = orderService.placeOrder(request);
        OrderResponse response = responseMapper.toOrderResponse(savedOrder);

        URI location = uriBuilder
                .path("/api/v1/orders/{id}")
                .buildAndExpand(savedOrder.getIdOrder())
                .toUri();

        return ResponseEntity.created(location).body(response);
    }

    @Operation(
            summary = "List all orders",
            security = @SecurityRequirement(name = SECURITY_SCHEME_NAME)
    )
    @GetMapping
    public ResponseEntity<List<OrderResponse>> getAllOrders() {
        List<Order> orders = orderService.getAllOrders();
        return ResponseEntity.ok(responseMapper.toOrderResponseList(orders));
    }

    @Operation(
            summary = "Get a specific order using its id",
            security = @SecurityRequirement(name = SECURITY_SCHEME_NAME)
    )
    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrderById(@PathVariable Long id) {
        Order order = orderService.getOrderById(id);
        return ResponseEntity.ok(responseMapper.toOrderResponse(order));
    }

    @Operation(
            summary = "Update an order",
            security = @SecurityRequirement(name = SECURITY_SCHEME_NAME)
    )
    @PutMapping("/{id}")
    public ResponseEntity<OrderResponse> updateOrder(@PathVariable Long id, @Valid @RequestBody OrderUpdateRequest request) {
        Order updatedOrder = orderService.updateOrder(id, request);
        return ResponseEntity.ok(responseMapper.toOrderResponse(updatedOrder));
    }

    @Operation(
            summary = "Partially update an order",
            security = @SecurityRequirement(name = SECURITY_SCHEME_NAME)
    )
    @PatchMapping("/{id}")
    public ResponseEntity<OrderResponse> partiallyUpdateOrder(@PathVariable Long id, @Valid @RequestBody OrderUpdateRequest request) {
        Order updatedOrder = orderService.updateOrder(id, request);
        return ResponseEntity.ok(responseMapper.toOrderResponse(updatedOrder));
    }

    @Operation(
            summary = "Delete an order based on its id",
            security = @SecurityRequirement(name = SECURITY_SCHEME_NAME)
    )
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOrder(@PathVariable Long id) {
        orderService.deleteOrder(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<OrderResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody OrderStatusUpdateRequestDto request) {
        return ResponseEntity.ok(orderService.updateStatus(id, request.getNewStatus(), request.getReason()));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<OrderResponse> cancelOrder(
            @PathVariable Long id,
            @RequestParam(required = false) String reason) {
        return ResponseEntity.ok(orderService.cancelOrder(id, reason));
    }

    @PostMapping("/{id}/apply-coupon")
    public ResponseEntity<OrderResponse> applyCoupon(
            @PathVariable Long id,
            @RequestParam String code) {
        return ResponseEntity.ok(orderService.applyCoupon(id, code));
    }
}
