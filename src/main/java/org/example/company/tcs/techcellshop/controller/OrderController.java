package org.example.company.tcs.techcellshop.controller;

import jakarta.validation.Valid;
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

    @PostMapping
    public ResponseEntity<OrderResponse> saveOrder(@Valid @RequestBody OrderEnrollmentRequest request, UriComponentsBuilder uriBuilder) {
        Order savedOrder = orderService.placeOrder(request);
        OrderResponse response = responseMapper.toOrderResponse(savedOrder);

        URI location = uriBuilder
                .path("/api/v1/orders/{id}")
                .buildAndExpand(savedOrder.getIdOrder())
                .toUri();

        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    public ResponseEntity<List<OrderResponse>> getAllOrders() {
        List<Order> orders = orderService.getAllOrders();
        return ResponseEntity.ok(responseMapper.toOrderResponseList(orders));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrderById(@PathVariable Long id) {
        Order order = orderService.getOrderById(id);
        return ResponseEntity.ok(responseMapper.toOrderResponse(order));
    }

    @PutMapping("/{id}")
    public ResponseEntity<OrderResponse> updateOrder(@PathVariable Long id, @Valid @RequestBody OrderUpdateRequest request) {
        Order updatedOrder = orderService.updateOrder(id, request);
        return ResponseEntity.ok(responseMapper.toOrderResponse(updatedOrder));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<OrderResponse> partiallyUpdateOrder(@PathVariable Long id, @Valid @RequestBody OrderUpdateRequest request) {
        Order updatedOrder = orderService.updateOrder(id, request);
        return ResponseEntity.ok(responseMapper.toOrderResponse(updatedOrder));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOrder(@PathVariable Long id) {
        orderService.deleteOrder(id);
        return ResponseEntity.noContent().build();
    }
}
