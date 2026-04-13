package org.example.company.tcs.techcellshop.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.company.tcs.techcellshop.domain.OutboxEvent;
import org.example.company.tcs.techcellshop.messaging.OrderCreatedEvent;
import org.example.company.tcs.techcellshop.repository.OutboxEventRepository;
import org.example.company.tcs.techcellshop.repository.ProcessedEventRepository;
import org.example.company.tcs.techcellshop.util.OrderStatus;
import org.example.company.tcs.techcellshop.util.OutboxEventStatus;
import org.example.company.tcs.techcellshop.util.PaymentStatus;
import org.junit.jupiter.api.BeforeEach;
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
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("it")
@DisplayName("Outbox Recovery Integration Tests")
class OutboxRecoveryIT extends AbstractMultiContainerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private ProcessedEventRepository processedEventRepository;

    @BeforeEach
    void setUp() {
        processedEventRepository.deleteAll();
        outboxEventRepository.deleteAll();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should requeue a failed event and eventually publish and process it")
    void shouldRequeueSingleFailedEventAndEventuallyPublishIt() throws Exception {
        OutboxEvent failedEvent = outboxEventRepository.save(createFailedEvent(101L));
        String eventId = failedEvent.getEventId();

        mockMvc.perform(post("/api/v1/admin/outbox/{id}/requeue", failedEvent.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestedIds[0]").value(failedEvent.getId()))
                .andExpect(jsonPath("$.requeuedCount").value(1))
                .andExpect(jsonPath("$.notFoundIds.length()").value(0))
                .andExpect(jsonPath("$.ignoredIds.length()").value(0));

        await()
                .atMost(15, TimeUnit.SECONDS)
                .pollInterval(1, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    OutboxEvent reloaded = outboxEventRepository.findById(failedEvent.getId()).orElseThrow();

                    assertThat(reloaded.getStatus()).isEqualTo(OutboxEventStatus.SENT);
                    assertThat(reloaded.getAttempts()).isZero();
                    assertThat(reloaded.getLastError()).isNull();
                    assertThat(reloaded.getSentAt()).isNotNull();

                    assertThat(processedEventRepository.existsByEventId(eventId)).isTrue();
                });
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should requeue only failed events in batch and eventually publish them")
    void shouldRequeueOnlyFailedEventsInBatchAndEventuallyPublishThem() throws Exception {
        OutboxEvent failedEvent1 = outboxEventRepository.save(createFailedEvent(201L));
        OutboxEvent failedEvent2 = outboxEventRepository.save(createFailedEvent(202L));
        OutboxEvent sentEvent = outboxEventRepository.save(createSentEvent(203L));

        long missingId = 999_999L;

        String requestBody = """
                {
                  "ids": [%d, %d, %d, %d]
                }
                """.formatted(
                failedEvent1.getId(),
                sentEvent.getId(),
                missingId,
                failedEvent2.getId()
        );

        mockMvc.perform(post("/api/v1/admin/outbox/requeue")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requeuedCount").value(2))
                .andExpect(jsonPath("$.notFoundIds[0]").value((int) missingId))
                .andExpect(jsonPath("$.ignoredIds[0]").value(sentEvent.getId()));

        await()
                .atMost(15, TimeUnit.SECONDS)
                .pollInterval(1, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    OutboxEvent reloaded1 = outboxEventRepository.findById(failedEvent1.getId()).orElseThrow();
                    OutboxEvent reloaded2 = outboxEventRepository.findById(failedEvent2.getId()).orElseThrow();
                    OutboxEvent untouchedSent = outboxEventRepository.findById(sentEvent.getId()).orElseThrow();

                    assertThat(reloaded1.getStatus()).isEqualTo(OutboxEventStatus.SENT);
                    assertThat(reloaded1.getSentAt()).isNotNull();
                    assertThat(reloaded1.getLastError()).isNull();

                    assertThat(reloaded2.getStatus()).isEqualTo(OutboxEventStatus.SENT);
                    assertThat(reloaded2.getSentAt()).isNotNull();
                    assertThat(reloaded2.getLastError()).isNull();

                    assertThat(untouchedSent.getStatus()).isEqualTo(OutboxEventStatus.SENT);

                    assertThat(processedEventRepository.existsByEventId(failedEvent1.getEventId())).isTrue();
                    assertThat(processedEventRepository.existsByEventId(failedEvent2.getEventId())).isTrue();
                    assertThat(processedEventRepository.existsByEventId(sentEvent.getEventId())).isFalse();
                });
    }

    private OutboxEvent createFailedEvent(Long orderId) throws Exception {
        String eventId = "evt-recovery-" + UUID.randomUUID();

        OrderCreatedEvent payload = new OrderCreatedEvent(
                eventId,
                orderId,
                1_000L + orderId,
                2_000L + orderId,
                1,
                new BigDecimal("3999.90"),
                "PIX",
                OrderStatus.CREATED,
                PaymentStatus.PENDING,
                Instant.now()
        );

        OutboxEvent event = new OutboxEvent();
        event.setEventId(eventId);
        event.setEventType("order.created");
        event.setAggregateType("Order");
        event.setAggregateId(orderId);
        event.setPayload(objectMapper.writeValueAsString(payload));
        event.setStatus(OutboxEventStatus.FAILED);
        event.setAttempts(5);
        event.setLastError("broker unavailable");
        event.setCreatedAt(Instant.now().minusSeconds(3600));
        event.setNextAttemptAt(Instant.now().minusSeconds(1800));
        return event;
    }

    private OutboxEvent createSentEvent(Long orderId) throws Exception {
        String eventId = "evt-sent-" + UUID.randomUUID();

        OrderCreatedEvent payload = new OrderCreatedEvent(
                eventId,
                orderId,
                3_000L + orderId,
                4_000L + orderId,
                1,
                new BigDecimal("2499.90"),
                "CREDIT_CARD",
                OrderStatus.CREATED,
                PaymentStatus.PENDING,
                Instant.now()
        );

        OutboxEvent event = new OutboxEvent();
        event.setEventId(eventId);
        event.setEventType("order.created");
        event.setAggregateType("Order");
        event.setAggregateId(orderId);
        event.setPayload(objectMapper.writeValueAsString(payload));
        event.setStatus(OutboxEventStatus.SENT);
        event.setAttempts(1);
        event.setLastError(null);
        event.setCreatedAt(Instant.now().minusSeconds(3600));
        event.setNextAttemptAt(Instant.now().minusSeconds(1800));
        event.setSentAt(Instant.now().minusSeconds(120));
        return event;
    }
}