package org.example.company.tcs.techcellshop.controller;

import jakarta.validation.Valid;
import org.example.company.tcs.techcellshop.controller.dto.OrderEnrollmentRequest;
import org.example.company.tcs.techcellshop.domain.Device;
import org.example.company.tcs.techcellshop.domain.Order;
import org.example.company.tcs.techcellshop.domain.User;
import org.example.company.tcs.techcellshop.service.OrderServiceImpl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/order")
public class OrderController {

    private final OrderServiceImpl orderService;

    OrderController(OrderServiceImpl orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<Order> saveOrder(@Valid @RequestBody OrderEnrollmentRequest request) {

        User user = new User();
        user.setIdUser(request.getIdUser());

        Device device = new Device();
        device.setIdDevice(request.getIdDevice());

        Order order = new Order();
        order.setUser(user);
        order.setDevice(device);
        order.setQuantityOrder(request.getQuantityOrder());
        order.setTotalPriceOrder(request.getTotalPriceOrder());
        order.setStatusOrder(request.getStatusOrder());
        order.setOrderDate(request.getOrderDate());
        order.setDeliveryDate(request.getDeliveryDate());
        order.setPaymentMethod(request.getPaymentMethod());
        order.setPaymentStatus(request.getPaymentStatus());

        return orderService.saveOrder(order);
    }

    @GetMapping
    public ResponseEntity<?> getAllOrders() {
        return orderService.getAllOrders();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Order> getOrderById(@PathVariable Long id) {
        return orderService.getOrderById(id);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Order> updateOrder(@PathVariable Long id, @RequestBody Order order) {
        return orderService.updateOrder(id, order);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOrder(@PathVariable Long id) {
        try {
            orderService.getOrderById(id);
            orderService.deleteOrder(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }
}
