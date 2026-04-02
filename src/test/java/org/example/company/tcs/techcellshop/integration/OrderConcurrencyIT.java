package org.example.company.tcs.techcellshop.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.company.tcs.techcellshop.controller.dto.request.OrderEnrollmentRequest;
import org.example.company.tcs.techcellshop.domain.Device;
import org.example.company.tcs.techcellshop.domain.User;
import org.example.company.tcs.techcellshop.repository.DeviceRepository;
import org.example.company.tcs.techcellshop.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.*;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("it")
class OrderConcurrencyIT extends AbstractPostgresIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private DeviceRepository deviceRepository;

    @Test
    @WithMockUser
    void placeOrder_concurrentRequests_stockOne_onlyOneShouldSucceed() throws Exception {
        User u = new User();
        u.setNameUser("Ana");
        u.setEmailUser("ana_it@techcellshop.com");
        u.setPasswordUser("123456");
        u.setPhoneUser("+55 11 90000-1234");
        u.setAddressUser("Sao Paulo");
        u.setRoleUser("USER");
        u = userRepository.save(u);

        Device d = new Device();
        d.setNameDevice("Load Test Phone");
        d.setDescriptionDevice("test");
        d.setDeviceType("SMARTPHONE");
        d.setDeviceStorage("128GB");
        d.setDeviceRam("8GB");
        d.setDeviceColor("Black");
        d.setDevicePrice(1000.0);
        d.setDeviceStock(1);
        d.setDeviceCondition("NEW");
        d = deviceRepository.save(d);

        OrderEnrollmentRequest req = new OrderEnrollmentRequest();
        req.setIdUser(u.getIdUser());
        req.setIdDevice(d.getIdDevice());
        req.setQuantityOrder(1);
        req.setPaymentMethod("PIX");
        String payload = objectMapper.writeValueAsString(req);

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Integer>> futures = new ArrayList<>();

        for (int i = 0; i < 2; i++) {
            futures.add(pool.submit(() -> {
                start.await();
                return mockMvc.perform(post("/api/v1/orders")
                                .header("Idempotency-Key", "stock-test-" + ThreadLocalRandom.current().nextInt())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(payload))
                        .andReturn()
                        .getResponse()
                        .getStatus();
            }));
        }

        start.countDown();

        int okOrCreated = 0;
        int conflictOrBadRequest = 0;

        for (Future<Integer> f : futures) {
            int status = f.get(20, TimeUnit.SECONDS);
            if (status == 201 || status == 200) okOrCreated++;
            if (status == 409 || status == 400) conflictOrBadRequest++;
        }

        pool.shutdownNow();

        Device reloaded = deviceRepository.findById(d.getIdDevice()).orElseThrow();
        assertThat(okOrCreated).isEqualTo(1);
        assertThat(conflictOrBadRequest).isEqualTo(1);
        assertThat(reloaded.getDeviceStock()).isEqualTo(0);
    }
}