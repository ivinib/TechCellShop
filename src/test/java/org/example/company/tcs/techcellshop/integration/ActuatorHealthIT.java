package org.example.company.tcs.techcellshop.integration;

import org.example.company.tcs.techcellshop.domain.OutboxEvent;
import org.example.company.tcs.techcellshop.repository.OutboxEventRepository;
import org.example.company.tcs.techcellshop.repository.ProcessedEventRepository;
import org.example.company.tcs.techcellshop.util.OutboxEventStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "management.endpoint.health.show-details=always",
        "management.endpoints.web.exposure.include=health,info,metrics",
        "management.endpoint.health.group.readiness.include=readinessState,db,orderFlow",
        "app.outbox.failed-threshold=0"
})
@DisplayName("Actuator Health Integration Tests")
class ActuatorHealthIT extends AbstractMultiContainerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private ProcessedEventRepository processedEventRepository;

    @BeforeEach
    void cleanUp() {
        outboxEventRepository.deleteAll();
        processedEventRepository.deleteAll();
    }

    @Test
    @DisplayName("Should expose orderFlow health details")
    void shouldExposeOrderFlowHealthDetails() throws Exception {
        mockMvc.perform(get("/actuator/health/orderFlow"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.details.processedEvents").exists())
                .andExpect(jsonPath("$.details.outboxPending").exists())
                .andExpect(jsonPath("$.details.outboxFailed").exists())
                .andExpect(jsonPath("$.details.failedThreshold").exists())
                .andExpect(jsonPath("$.details.oldestPendingAgeSeconds").exists());
    }

    @Test
    @DisplayName("Should include orderFlow component in readiness group")
    void shouldIncludeOrderFlowInReadiness() throws Exception {
        mockMvc.perform(get("/actuator/health/readiness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components.orderFlow").exists())
                .andExpect(jsonPath("$.components.orderFlow.status").exists());
    }

    @Test
    @DisplayName("Should mark orderFlow DOWN when failed outbox exceeds threshold")
    void shouldReturnDownWhenFailedOutboxExceedsThreshold() throws Exception {
        OutboxEvent failed = new OutboxEvent();
        failed.setEventId(UUID.randomUUID().toString());
        failed.setEventType("order.created");
        failed.setAggregateType("Order");
        failed.setAggregateId(999L);
        failed.setPayload("{\"eventId\":\"" + UUID.randomUUID() + "\"}");
        failed.setStatus(OutboxEventStatus.FAILED);
        failed.setAttempts(5);
        failed.setCreatedAt(Instant.now().minusSeconds(60));
        failed.setNextAttemptAt(Instant.now());
        failed.setLastError("forced failure for test");
        outboxEventRepository.save(failed);

        mockMvc.perform(get("/actuator/health/orderFlow"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value("DOWN"))
                .andExpect(jsonPath("$.details.outboxFailed").value(1));
    }
}