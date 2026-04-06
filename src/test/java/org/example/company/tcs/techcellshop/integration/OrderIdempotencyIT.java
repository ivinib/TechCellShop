package org.example.company.tcs.techcellshop.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.company.tcs.techcellshop.dto.request.OrderEnrollmentRequest;
import org.example.company.tcs.techcellshop.domain.Device;
import org.example.company.tcs.techcellshop.domain.User;
import org.example.company.tcs.techcellshop.repository.DeviceRepository;
import org.example.company.tcs.techcellshop.repository.OrderIdempondencyRepository;
import org.example.company.tcs.techcellshop.repository.OrderRepository;
import org.example.company.tcs.techcellshop.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("it")
class OrderIdempotencyIT extends AbstractPostgresIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private DeviceRepository deviceRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private OrderIdempondencyRepository orderIdempondencyRepository;

    @Test
    @WithMockUser
    void placeOrder_sameIdempotencyKey_concurrentRequests_shouldReturnSameOrder() throws Exception {
        User user = new User();
        user.setNameUser("Ana Idempotency");
        user.setEmailUser("ana_idem_it@techcellshop.com");
        user.setPasswordUser("123456");
        user.setPhoneUser("+55 11 90000-7777");
        user.setAddressUser("Sao Paulo");
        user.setRoleUser("USER");
        user = userRepository.save(user);

        Device device = new Device();
        device.setNameDevice("Idempotency Test Phone");
        device.setDescriptionDevice("Device for idempotency integration test");
        device.setDeviceType("SMARTPHONE");
        device.setDeviceStorage("128GB");
        device.setDeviceRam("8GB");
        device.setDeviceColor("Black");
        device.setDevicePrice(1200.0);
        device.setDeviceStock(10);
        device.setDeviceCondition("NEW");
        device = deviceRepository.save(device);

        OrderEnrollmentRequest request = new OrderEnrollmentRequest();
        request.setIdUser(user.getIdUser());
        request.setIdDevice(device.getIdDevice());
        request.setQuantityOrder(1);
        request.setPaymentMethod("PIX");

        String payload = objectMapper.writeValueAsString(request);
        String sameKey = "same-idempotency-key";

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Long>> futures = new ArrayList<>();

        for (int i = 0; i < 2; i++) {
            futures.add(pool.submit(() -> {
                start.await();

                String responseBody = mockMvc.perform(post("/api/v1/orders")
                                .header("Idempotency-Key", sameKey)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(payload))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

                JsonNode json = objectMapper.readTree(responseBody);
                return json.get("idOrder").asLong();
            }));
        }

        start.countDown();

        Long id1 = futures.get(0).get(20, TimeUnit.SECONDS);
        Long id2 = futures.get(1).get(20, TimeUnit.SECONDS);

        pool.shutdownNow();

        assertThat(id1).isEqualTo(id2);
        assertThat(orderRepository.count()).isEqualTo(1);
        assertThat(orderIdempondencyRepository.findByIdempotencyKey(sameKey)).isPresent();
    }
}