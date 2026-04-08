package org.example.company.tcs.techcellshop.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.company.tcs.techcellshop.domain.Device;
import org.example.company.tcs.techcellshop.domain.OutboxEvent;
import org.example.company.tcs.techcellshop.domain.User;
import org.example.company.tcs.techcellshop.dto.request.OrderEnrollmentRequest;
import org.example.company.tcs.techcellshop.repository.DeviceRepository;
import org.example.company.tcs.techcellshop.repository.OutboxEventRepository;
import org.example.company.tcs.techcellshop.repository.UserRepository;
import org.example.company.tcs.techcellshop.util.OutboxEventStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("it")
@DisplayName("Outbox Event Publishing Integration Tests")
class OutboxEventPublishingIT extends AbstractMultiContainerIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private DeviceRepository deviceRepository;
    @Autowired private OutboxEventRepository outboxEventRepository;

    @Test
    @WithMockUser
    @DisplayName("Should persist outbox event before publishing")
    void placeOrder_shouldPersistOutboxBeforePublish() throws Exception {
        User user = createTestUser("outbox_test@techcellshop.com");
        Device device = createTestDevice();

        OrderEnrollmentRequest request = new OrderEnrollmentRequest();
        request.setIdUser(user.getIdUser());
        request.setIdDevice(device.getIdDevice());
        request.setQuantityOrder(1);
        request.setPaymentMethod("PIX");

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        var events = outboxEventRepository.findByStatusOrderByCreatedAtDesc(
                OutboxEventStatus.PENDING,
                PageRequest.of(0, 10)
        ).getContent();

        assertThat(events)
                .isNotEmpty()
                .anySatisfy(event -> {
                    assertThat(event.getEventType()).isEqualTo("OrderCreated");
                    assertThat(event.getAggregateType()).isEqualTo("Order");
                    assertThat(event.getStatus()).isEqualTo(OutboxEventStatus.PENDING);
                });
    }

    @Test
    @WithMockUser
    @DisplayName("Should eventually mark event as sent")
    void outboxEvent_shouldBeSentEventually() throws Exception {
        User user = createTestUser("outbox_publish@techcellshop.com");
        Device device = createTestDevice();

        OrderEnrollmentRequest request = new OrderEnrollmentRequest();
        request.setIdUser(user.getIdUser());
        request.setIdDevice(device.getIdDevice());
        request.setQuantityOrder(1);
        request.setPaymentMethod("PIX");

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        await()
                .atMost(15, TimeUnit.SECONDS)
                .pollInterval(2, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    var sentEvents = outboxEventRepository.findByStatusOrderByCreatedAtDesc(
                            OutboxEventStatus.SENT,
                            PageRequest.of(0, 10)
                    ).getContent();
                    assertThat(sentEvents).isNotEmpty();
                });
    }

    @Test
    @DisplayName("Should verify failed outbox events are stored")
    void failedOutboxEvent_shouldBeMarkedAsFailed() {
        OutboxEvent failedEvent = new OutboxEvent();
        failedEvent.setEventId("test-event-" + System.currentTimeMillis());
        failedEvent.setEventType("OrderCreated");
        failedEvent.setAggregateType("Order");
        failedEvent.setAggregateId(1L);
        failedEvent.setPayload("{\"orderId\": 1}");
        failedEvent.setStatus(OutboxEventStatus.FAILED);
        failedEvent.setAttempts(3);
        failedEvent.setNextAttemptAt(Instant.now().minusSeconds(60));
        failedEvent.setCreatedAt(Instant.now());
        failedEvent.setLastError("Connection timeout");

        failedEvent = outboxEventRepository.save(failedEvent);

        var event = outboxEventRepository.findById(failedEvent.getId()).orElseThrow();
        assertThat(event.getStatus()).isEqualTo(OutboxEventStatus.FAILED);
        assertThat(event.getAttempts()).isEqualTo(3);
        assertThat(event.getLastError()).contains("timeout");
    }

    private User createTestUser(String email) {
        User user = new User();
        user.setNameUser("Outbox Test User");
        user.setEmailUser(email);
        user.setPasswordUser("123456");
        user.setPhoneUser("+55 11 90000-2222");
        user.setAddressUser("Test City");
        user.setRoleUser("USER");
        return userRepository.save(user);
    }

    private Device createTestDevice() {
        Device device = new Device();
        device.setNameDevice("Outbox Test Device");
        device.setDescriptionDevice("Device for outbox testing");
        device.setDeviceType("SMARTPHONE");
        device.setDeviceStorage("256GB");
        device.setDeviceRam("12GB");
        device.setDeviceColor("White");
        device.setDevicePrice(money("3299.90"));
        device.setDeviceStock(100);
        device.setDeviceCondition("NEW");
        return deviceRepository.save(device);
    }

    private BigDecimal money(String value) {
        return new BigDecimal(value);
    }
}
