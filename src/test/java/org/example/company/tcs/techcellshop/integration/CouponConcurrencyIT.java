package org.example.company.tcs.techcellshop.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.company.tcs.techcellshop.dto.request.OrderEnrollmentRequest;
import org.example.company.tcs.techcellshop.domain.Coupon;
import org.example.company.tcs.techcellshop.domain.Device;
import org.example.company.tcs.techcellshop.domain.User;
import org.example.company.tcs.techcellshop.repository.CouponRepository;
import org.example.company.tcs.techcellshop.repository.DeviceRepository;
import org.example.company.tcs.techcellshop.repository.UserRepository;
import org.example.company.tcs.techcellshop.util.DiscountType;
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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("it")
class CouponConcurrencyIT extends AbstractPostgresIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private DeviceRepository deviceRepository;
    @Autowired private CouponRepository couponRepository;

    @Test
    @WithMockUser(username = "ana_coupon_it@techcellshop.com", roles = "USER")
    void applyCoupon_concurrentRequests_maxUsesOne_onlyOneShouldSucceed() throws Exception {
        User user = new User();
        user.setNameUser("Ana Coupon");
        user.setEmailUser("ana_coupon_it@techcellshop.com");
        user.setPasswordUser("123456");
        user.setPhoneUser("+55 11 90000-9999");
        user.setAddressUser("Sao Paulo");
        user.setRoleUser("USER");
        user = userRepository.save(user);

        Device device = new Device();
        device.setNameDevice("Coupon Test Phone");
        device.setDescriptionDevice("Device for coupon concurrency test");
        device.setDeviceType("SMARTPHONE");
        device.setDeviceStorage("128GB");
        device.setDeviceRam("8GB");
        device.setDeviceColor("Black");
        device.setDevicePrice(new BigDecimal("1000.00"));
        device.setDeviceStock(5);
        device.setDeviceCondition("NEW");
        device = deviceRepository.save(device);

        Coupon coupon = new Coupon();
        coupon.setCode("OFF10");
        coupon.setType(DiscountType.PERCENT);
        coupon.setValue(new BigDecimal("10"));
        coupon.setActive(true);
        coupon.setStartsAt(OffsetDateTime.now().minusDays(1));
        coupon.setEndsAt(OffsetDateTime.now().plusDays(7));
        coupon.setMinOrderAmount(new BigDecimal("100"));
        coupon.setMaxUses(1);
        coupon.setUsedCount(0);
        couponRepository.save(coupon);

        Long orderId1 = createOrder(device.getIdDevice(), "coupon-key-1");
        Long orderId2 = createOrder(device.getIdDevice(), "coupon-key-2");

        CountDownLatch start = new CountDownLatch(1);
        List<Future<Integer>> futures = new ArrayList<>();

        try (ExecutorService pool = Executors.newFixedThreadPool(2)) {
            futures.add(pool.submit(() -> {
                start.await();
                return mockMvc.perform(post("/api/v1/orders/" + orderId1 + "/apply-coupon")
                                .param("code", "OFF10"))
                        .andReturn()
                        .getResponse()
                        .getStatus();
            }));

            futures.add(pool.submit(() -> {
                start.await();
                return mockMvc.perform(post("/api/v1/orders/" + orderId2 + "/apply-coupon")
                                .param("code", "OFF10"))
                        .andReturn()
                        .getResponse()
                        .getStatus();
            }));

            start.countDown();

            int successCount = 0;
            int conflictCount = 0;

            for (Future<Integer> future : futures) {
                int status = future.get(20, TimeUnit.SECONDS);
                if (status == 200) {
                    successCount++;
                }
                if (status == 409) {
                    conflictCount++;
                }
            }

            Coupon reloaded = couponRepository.findByCodeIgnoreCase("OFF10").orElseThrow();

            assertThat(successCount).isEqualTo(1);
            assertThat(conflictCount).isEqualTo(1);
            assertThat(reloaded.getUsedCount()).isEqualTo(1);
        }
    }

    private Long createOrder(Long deviceId, String idempotencyKey) throws Exception {
        OrderEnrollmentRequest request = new OrderEnrollmentRequest();
        request.setIdDevice(deviceId);
        request.setQuantityOrder(1);
        request.setPaymentMethod("PIX");

        String response = mockMvc.perform(post("/api/v1/orders")
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode json = objectMapper.readTree(response);
        return json.get("idOrder").asLong();
    }
}