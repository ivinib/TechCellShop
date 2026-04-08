package org.example.company.tcs.techcellshop.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.company.tcs.techcellshop.domain.Device;
import org.example.company.tcs.techcellshop.domain.Order;
import org.example.company.tcs.techcellshop.domain.User;
import org.example.company.tcs.techcellshop.dto.order.OrderStatusUpdateRequestDto;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("it")
@DisplayName("Order Retry Logic Integration Tests")
class OrderRetryIT extends AbstractMultiContainerIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private DeviceRepository deviceRepository;
    @Autowired private OrderRepository orderRepository;

    @Test
    @WithMockUser
    @DisplayName("Should successfully update order with optimistic locking")
    void updateOrder_withOptimisticLocking_shouldSucceed() throws Exception {
        User user = createTestUser("retry_test@techcellshop.com");
        Device device = createTestDevice();

        Order order = new Order();
        order.setUser(user);
        order.setDevice(device);
        order.setQuantityOrder(1);
        order.setTotalPriceOrder(device.getDevicePrice());
        order.setStatus(OrderStatus.CREATED);
        order.setPaymentMethod("PIX");
        order.setPaymentStatus(PaymentStatus.PENDING);
        order = orderRepository.save(order);

        Long originalVersion = order.getVersion();

        OrderStatusUpdateRequestDto request = new OrderStatusUpdateRequestDto();
        request.setNewStatus(OrderStatus.PAID);
        request.setReason("Test update");

        mockMvc.perform(patch("/api/v1/orders/{id}/status", order.getIdOrder())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"));

        Order updated = orderRepository.findById(order.getIdOrder()).orElseThrow();
        assertThat(updated.getVersion()).isGreaterThan(originalVersion);
    }

    @Test
    @WithMockUser
    @DisplayName("Should handle multiple concurrent order updates")
    void multipleOrderUpdates_concurrently_shouldAllSucceed() {
        User user = createTestUser("concurrent_test@techcellshop.com");
        Device device = createTestDevice();

        Order order = new Order();
        order.setUser(user);
        order.setDevice(device);
        order.setQuantityOrder(1);
        order.setTotalPriceOrder(device.getDevicePrice());
        order.setStatus(OrderStatus.CREATED);
        order.setPaymentMethod("PIX");
        order.setPaymentStatus(PaymentStatus.PENDING);
        order = orderRepository.save(order);

        Order retrieved = orderRepository.findById(order.getIdOrder()).orElseThrow();
        assertThat(retrieved.getStatus()).isEqualTo(OrderStatus.CREATED);
        assertThat(retrieved.getVersion()).isNotNull();
        assertThat(retrieved.getVersion()).isEqualTo(0L);
    }

    @Test
    @WithMockUser
    @DisplayName("Should retrieve order with version information")
    void getOrder_shouldIncludeVersionInfo() {
        User user = createTestUser("version_test@techcellshop.com");
        Device device = createTestDevice();

        Order order = new Order();
        order.setUser(user);
        order.setDevice(device);
        order.setQuantityOrder(2);
        order.setTotalPriceOrder(device.getDevicePrice().multiply(BigDecimal.valueOf(2)));
        order.setStatus(OrderStatus.CREATED);
        order.setPaymentMethod("PIX");
        order.setPaymentStatus(PaymentStatus.PENDING);
        order = orderRepository.save(order);

        Order retrieved = orderRepository.findById(order.getIdOrder()).orElseThrow();
        assertThat(retrieved.getIdOrder()).isEqualTo(order.getIdOrder());
        assertThat(retrieved.getQuantityOrder()).isEqualTo(2);
        assertThat(retrieved.getVersion()).isNotNull();
        assertThat(retrieved.getTotalPriceOrder()).isEqualByComparingTo("5599.80");
    }

    private User createTestUser(String email) {
        User user = new User();
        user.setNameUser("Retry Test User");
        user.setEmailUser(email);
        user.setPasswordUser("123456");
        user.setPhoneUser("+55 11 90000-3333");
        user.setAddressUser("Test City");
        user.setRoleUser("USER");
        return userRepository.save(user);
    }

    private Device createTestDevice() {
        Device device = new Device();
        device.setNameDevice("Retry Test Device");
        device.setDescriptionDevice("Device for retry testing");
        device.setDeviceType("SMARTPHONE");
        device.setDeviceStorage("256GB");
        device.setDeviceRam("12GB");
        device.setDeviceColor("Gold");
        device.setDevicePrice(money("2799.90"));
        device.setDeviceStock(100);
        device.setDeviceCondition("NEW");
        return deviceRepository.save(device);
    }

    private BigDecimal money(String value) {
        return new BigDecimal(value);
    }
}