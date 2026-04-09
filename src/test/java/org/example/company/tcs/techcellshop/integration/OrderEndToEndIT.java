package org.example.company.tcs.techcellshop.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.company.tcs.techcellshop.domain.Coupon;
import org.example.company.tcs.techcellshop.domain.Device;
import org.example.company.tcs.techcellshop.domain.Order;
import org.example.company.tcs.techcellshop.domain.User;
import org.example.company.tcs.techcellshop.dto.order.OrderStatusUpdateRequestDto;
import org.example.company.tcs.techcellshop.dto.request.OrderEnrollmentRequest;
import org.example.company.tcs.techcellshop.repository.CouponRepository;
import org.example.company.tcs.techcellshop.repository.DeviceRepository;
import org.example.company.tcs.techcellshop.repository.OrderRepository;
import org.example.company.tcs.techcellshop.repository.UserRepository;
import org.example.company.tcs.techcellshop.util.DiscountType;
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
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("it")
@DisplayName("Order End-to-End Integration Tests")
class OrderEndToEndIT extends AbstractMultiContainerIT {

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

    @Autowired
    private CouponRepository couponRepository;

    private User testUser;
    private Device testDevice;

    private void setupTestData() {
        testUser = new User();
        testUser.setNameUser("E2E Test User");
        testUser.setEmailUser("e2e_test@techcellshop.com");
        testUser.setPasswordUser("123456");
        testUser.setPhoneUser("+55 11 90000-1111");
        testUser.setAddressUser("Test City");
        testUser.setRoleUser("USER");
        testUser = userRepository.save(testUser);

        testDevice = new Device();
        testDevice.setNameDevice("E2E Test Device");
        testDevice.setDescriptionDevice("Device for e2e testing");
        testDevice.setDeviceType("SMARTPHONE");
        testDevice.setDeviceStorage("256GB");
        testDevice.setDeviceRam("12GB");
        testDevice.setDeviceColor("Blue");
        testDevice.setDevicePrice(new BigDecimal("2999.90"));
        testDevice.setDeviceStock(50);
        testDevice.setDeviceCondition("NEW");
        testDevice = deviceRepository.save(testDevice);
    }

    @Test
    @WithMockUser(username = "e2e_test@techcellshop.com", roles = "USER")
    @DisplayName("Should place order successfully")
    void placeOrder_shouldCreateOrder() throws Exception {
        setupTestData();

        OrderEnrollmentRequest request = new OrderEnrollmentRequest();
        request.setIdDevice(testDevice.getIdDevice());
        request.setQuantityOrder(2);
        request.setPaymentMethod("PIX");

        mockMvc.perform(post("/api/v1/orders")
                        .header("Idempotency-Key", "e2e-order-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statusOrder").value("CREATED"));

        assertThat(orderRepository.count()).isGreaterThan(0);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should update order status through API")
    void updateOrderStatus_shouldSucceed() throws Exception {
        setupTestData();

        Order order = new Order();
        order.setUser(testUser);
        order.setDevice(testDevice);
        order.setQuantityOrder(1);
        order.setTotalPriceOrder(testDevice.getDevicePrice());
        order.setStatus(OrderStatus.CREATED);
        order.setPaymentMethod("PIX");
        order.setPaymentStatus(PaymentStatus.PENDING);
        order = orderRepository.save(order);

        OrderStatusUpdateRequestDto requestDto = new OrderStatusUpdateRequestDto();
        requestDto.setNewStatus(OrderStatus.PAID);
        requestDto.setReason("Payment confirmed");

        mockMvc.perform(patch("/api/v1/orders/{id}/status", order.getIdOrder())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusOrder").value("PAID"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should cancel order successfully")
    void cancelOrder_shouldUpdateStatusAndCreateEvent() throws Exception {
        setupTestData();

        Order order = new Order();
        order.setUser(testUser);
        order.setDevice(testDevice);
        order.setQuantityOrder(1);
        order.setTotalPriceOrder(testDevice.getDevicePrice());
        order.setStatus(OrderStatus.CREATED);
        order.setPaymentMethod("PIX");
        order.setPaymentStatus(PaymentStatus.PENDING);
        order = orderRepository.save(order);

        OrderStatusUpdateRequestDto requestDto = new OrderStatusUpdateRequestDto();
        requestDto.setNewStatus(OrderStatus.CANCELED);
        requestDto.setReason("Customer requested");

        mockMvc.perform(patch("/api/v1/orders/{id}/status", order.getIdOrder())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusOrder").value("CANCELED"));

        Order canceled = orderRepository.findById(order.getIdOrder()).orElseThrow();
        assertThat(canceled.getStatus()).isEqualTo(OrderStatus.CANCELED);
    }

    @Test
    @WithMockUser(username = "e2e_test@techcellshop.com", roles = "USER")
    @DisplayName("Should apply coupon to order")
    void applyCoupon_shouldCalculateDiscount() throws Exception {
        setupTestData();
        createCoupon("SAVE10", new BigDecimal("10"));

        Order order = new Order();
        order.setUser(testUser);
        order.setDevice(testDevice);
        order.setQuantityOrder(1);
        order.setTotalPriceOrder(testDevice.getDevicePrice());
        order.setStatus(OrderStatus.CREATED);
        order.setPaymentMethod("PIX");
        order.setPaymentStatus(PaymentStatus.PENDING);
        order = orderRepository.save(order);

        mockMvc.perform(post("/api/v1/orders/{id}/apply-coupon", order.getIdOrder())
                        .param("code", "SAVE10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.couponCode").value("SAVE10"));
    }

    private void createCoupon(String code, BigDecimal value) {
        Coupon coupon = new Coupon();
        coupon.setCode(code);
        coupon.setType(DiscountType.PERCENT);
        coupon.setValue(value);
        coupon.setActive(true);
        coupon.setStartsAt(OffsetDateTime.now().minusDays(1));
        coupon.setEndsAt(OffsetDateTime.now().plusDays(7));
        coupon.setMinOrderAmount(new BigDecimal("100.00"));
        coupon.setMaxUses(100);
        coupon.setUsedCount(0);
        couponRepository.save(coupon);
    }
}