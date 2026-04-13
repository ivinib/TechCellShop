package org.example.company.tcs.techcellshop.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.company.tcs.techcellshop.domain.Device;
import org.example.company.tcs.techcellshop.domain.Order;
import org.example.company.tcs.techcellshop.domain.User;
import org.example.company.tcs.techcellshop.dto.payment.PaymentActionRequestDto;
import org.example.company.tcs.techcellshop.dto.request.OrderEnrollmentRequest;
import org.example.company.tcs.techcellshop.repository.DeviceRepository;
import org.example.company.tcs.techcellshop.repository.OrderRepository;
import org.example.company.tcs.techcellshop.repository.UserRepository;
import org.example.company.tcs.techcellshop.util.OrderStatus;
import org.example.company.tcs.techcellshop.util.PaymentStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("it")
@DisplayName("Payment Flow Integration Tests")
class PaymentFlowIT extends AbstractPostgresIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Test
    @WithMockUser(username = "payment_admin_confirm@techcellshop.com", roles = "ADMIN")
    @DisplayName("Should confirm payment and mark order as paid")
    void confirmPayment_shouldMarkOrderAsPaid() throws Exception {
        User user = createTestUser("payment_admin_confirm@techcellshop.com", "ADMIN");
        Device device = createTestDevice(20);

        Order order = new Order();
        order.setUser(user);
        order.setDevice(device);
        order.setQuantityOrder(1);
        order.setTotalPriceOrder(money("2999.90"));
        order.setFinalAmount(money("2899.90"));
        order.setStatus(OrderStatus.CREATED);
        order.setPaymentMethod("PIX");
        order.setPaymentStatus(PaymentStatus.PENDING);
        order = orderRepository.save(order);

        PaymentActionRequestDto request = paymentRequest("TXN-1001", "2899.90", "Payment approved");

        mockMvc.perform(post("/api/v1/payments/orders/{id}/confirm", order.getIdOrder())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(order.getIdOrder()))
                .andExpect(jsonPath("$.paymentStatus").value("CONFIRMED"));

        Order updated = orderRepository.findById(order.getIdOrder()).orElseThrow();
        assertThat(updated.getPaymentStatus()).isEqualTo(PaymentStatus.CONFIRMED);
        assertThat(updated.getStatus()).isEqualTo(OrderStatus.PAID);
    }

    @Test
    @WithMockUser(username = "payment_admin_fail@techcellshop.com", roles = "ADMIN")
    @DisplayName("Should fail payment cancel order and restore stock")
    void failPayment_shouldCancelOrderAndReleaseStock() throws Exception {
        createTestUser("payment_admin_fail@techcellshop.com", "ADMIN");
        Device device = createTestDevice(20);

        Long orderId = createOrderThroughApi("payment_admin_fail@techcellshop.com", device.getIdDevice(), 2, "fail-key-001");

        Device reservedDevice = deviceRepository.findById(device.getIdDevice()).orElseThrow();
        assertThat(reservedDevice.getDeviceStock()).isEqualTo(18);

        PaymentActionRequestDto request = paymentRequest("TXN-1002", "5999.80", "Card denied");

        mockMvc.perform(post("/api/v1/payments/orders/{id}/fail", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(orderId))
                .andExpect(jsonPath("$.paymentStatus").value("FAILED"));

        Order updated = orderRepository.findById(orderId).orElseThrow();
        Device reloadedDevice = deviceRepository.findById(device.getIdDevice()).orElseThrow();

        assertThat(updated.getStatus()).isEqualTo(OrderStatus.CANCELED);
        assertThat(updated.getPaymentStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(updated.getCanceledReason()).isEqualTo("Card denied");
        assertThat(reloadedDevice.getDeviceStock()).isEqualTo(20);
    }

    @Test
    @WithMockUser(username = "payment_admin_refund@techcellshop.com", roles = "ADMIN")
    @DisplayName("Should refund payment for canceled order with confirmed payment")
    void refundPayment_shouldMarkPaymentAsRefunded() throws Exception {
        User user = createTestUser("payment_admin_refund@techcellshop.com", "ADMIN");
        Device device = createTestDevice(15);

        Order order = new Order();
        order.setUser(user);
        order.setDevice(device);
        order.setQuantityOrder(1);
        order.setTotalPriceOrder(money("2999.90"));
        order.setFinalAmount(money("2899.90"));
        order.setStatus(OrderStatus.CANCELED);
        order.setPaymentMethod("PIX");
        order.setPaymentStatus(PaymentStatus.CONFIRMED);
        order.setCanceledReason("Customer changed mind");
        order = orderRepository.save(order);

        PaymentActionRequestDto request = paymentRequest("TXN-1003", "2899.90", "Customer refunded");

        mockMvc.perform(post("/api/v1/payments/orders/{id}/refund", order.getIdOrder())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(order.getIdOrder()))
                .andExpect(jsonPath("$.paymentStatus").value("REFUNDED"));

        Order updated = orderRepository.findById(order.getIdOrder()).orElseThrow();
        assertThat(updated.getPaymentStatus()).isEqualTo(PaymentStatus.REFUNDED);
    }

    @Test
    @WithMockUser(username = "payment_admin_invalid_refund@techcellshop.com", roles = "ADMIN")
    @DisplayName("Should return 409 when refund is requested for non canceled order")
    void refundPayment_shouldReturn409WhenOrderStateIsInvalid() throws Exception {
        User user = createTestUser("payment_admin_invalid_refund@techcellshop.com", "ADMIN");
        Device device = createTestDevice(15);

        Order order = new Order();
        order.setUser(user);
        order.setDevice(device);
        order.setQuantityOrder(1);
        order.setTotalPriceOrder(money("2999.90"));
        order.setFinalAmount(money("2899.90"));
        order.setStatus(OrderStatus.CREATED);
        order.setPaymentMethod("PIX");
        order.setPaymentStatus(PaymentStatus.CONFIRMED);
        order = orderRepository.save(order);

        PaymentActionRequestDto request = paymentRequest("TXN-1004", "2899.90", "Invalid refund attempt");

        mockMvc.perform(post("/api/v1/payments/orders/{id}/refund", order.getIdOrder())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.code").value("BUSINESS_CONFLICT"))
                .andExpect(jsonPath("$.message").value("Cannot refund payment for an order that is not canceled"))
                .andExpect(jsonPath("$.path").value("/api/v1/payments/orders/" + order.getIdOrder() + "/refund"));
    }

    private Long createOrderThroughApi(String username, Long deviceId, int quantity, String idempotencyKey) throws Exception {
        createTestUser(username, "ADMIN");

        OrderEnrollmentRequest request = new OrderEnrollmentRequest();
        request.setIdDevice(deviceId);
        request.setQuantityOrder(quantity);
        request.setPaymentMethod("PIX");

        String response = mockMvc.perform(post("/api/v1/orders")
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode json = objectMapper.readTree(response);
        return json.get("idOrder").asLong();
    }

    private User createTestUser(String email, String role) {
        return userRepository.findByEmailUserIgnoreCase(email)
                .orElseGet(() -> {
                    User user = new User();
                    user.setNameUser("Payment Test User");
                    user.setEmailUser(email);
                    user.setPasswordUser("123456");
                    user.setPhoneUser("+55 11 90000-4444");
                    user.setAddressUser("Test City");
                    user.setRoleUser(role);
                    return userRepository.save(user);
                });
    }

    private Device createTestDevice(int stock) {
        Device device = new Device();
        device.setNameDevice("Payment Test Device " + System.nanoTime());
        device.setDescriptionDevice("Device for payment integration testing");
        device.setDeviceType("SMARTPHONE");
        device.setDeviceStorage("256GB");
        device.setDeviceRam("8GB");
        device.setDeviceColor("Black");
        device.setDevicePrice(money("2999.90"));
        device.setDeviceStock(stock);
        device.setDeviceCondition("NEW");
        return deviceRepository.save(device);
    }

    private PaymentActionRequestDto paymentRequest(String transactionId, String amount, String reason) {
        PaymentActionRequestDto request = new PaymentActionRequestDto();
        request.setTransactionId(transactionId);
        request.setAmount(money(amount));
        request.setReason(reason);
        return request;
    }

    private BigDecimal money(String value) {
        return new BigDecimal(value);
    }
}