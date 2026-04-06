package org.example.company.tcs.techcellshop.integration;

import org.example.company.tcs.techcellshop.domain.OutboxEvent;
import org.example.company.tcs.techcellshop.repository.OutboxEventRepository;
import org.example.company.tcs.techcellshop.util.OutboxEventStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Outbox Admin Integration")
class OutboxAdminIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    private OutboxEvent failedEvent;

    @BeforeEach
    void setUp() {
        outboxEventRepository.deleteAll();

        failedEvent = new OutboxEvent();
        failedEvent.setEventId("evt-failed-001");
        failedEvent.setEventType("order.created");
        failedEvent.setAggregateType("Order");
        failedEvent.setAggregateId(100L);
        failedEvent.setStatus(OutboxEventStatus.FAILED);
        failedEvent.setAttempts(5);
        failedEvent.setLastError("Connection timeout");
        failedEvent.setCreatedAt(Instant.now().minusSeconds(3600));
        failedEvent.setNextAttemptAt(Instant.now().minusSeconds(1800));
        failedEvent.setPayload("{}");

        outboxEventRepository.save(failedEvent);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("should list failed events as admin")
    void shouldListFailedEventsAsAdmin() throws Exception {
        mockMvc.perform(get("/api/v1/admin/outbox/failed?page=0&size=20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(failedEvent.getId()))
                .andExpect(jsonPath("$.content[0].status").value("FAILED"))
                .andExpect(jsonPath("$.content[0].attempts").value(5));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("should requeue single event as admin")
    void shouldRequeueSingleEventAsAdmin() throws Exception {
        mockMvc.perform(post("/api/v1/admin/outbox/{id}/requeue", failedEvent.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requeuedCount").value(1))
                .andExpect(jsonPath("$.notFoundIds").isArray())
                .andExpect(jsonPath("$.notFoundIds.length()").value(0));

        OutboxEvent requeued = outboxEventRepository.findById(failedEvent.getId()).orElseThrow();
        assertThat(requeued.getStatus()).isEqualTo(OutboxEventStatus.PENDING);
        assertThat(requeued.getAttempts()).isZero();
        assertThat(requeued.getLastError()).isNull();
        assertThat(requeued.getNextAttemptAt()).isNotNull();
        assertThat(requeued.getSentAt()).isNull();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("should requeue batch of events as admin")
    void shouldRequeueBatchAsAdmin() throws Exception {
        OutboxEvent failedEvent2 = new OutboxEvent();
        failedEvent2.setEventId("evt-failed-002");
        failedEvent2.setEventType("order.created");
        failedEvent2.setAggregateType("Order");
        failedEvent2.setAggregateId(101L);
        failedEvent2.setStatus(OutboxEventStatus.FAILED);
        failedEvent2.setAttempts(4);
        failedEvent2.setCreatedAt(Instant.now());
        failedEvent2.setNextAttemptAt(Instant.now());
        failedEvent2.setPayload("{}");

        OutboxEvent saved = outboxEventRepository.save(failedEvent2);

        String requestBody = "{\"ids\": [" + failedEvent.getId() + ", " + saved.getId() + "]}";

        mockMvc.perform(post("/api/v1/admin/outbox/requeue")
                        .contentType("application/json")
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requeuedCount").value(2))
                .andExpect(jsonPath("$.ignoredIds.length()").value(0));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("should forbid non-admin access")
    void shouldForbidNonAdminAccess() throws Exception {
        mockMvc.perform(get("/api/v1/admin/outbox/failed"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("should require authentication")
    void shouldRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/admin/outbox/failed"))
                .andExpect(status().isUnauthorized());
    }
}