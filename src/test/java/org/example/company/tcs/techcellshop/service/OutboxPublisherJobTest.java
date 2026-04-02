package org.example.company.tcs.techcellshop.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.example.company.tcs.techcellshop.domain.OutboxEvent;
import org.example.company.tcs.techcellshop.messaging.OrderCreatedEvent;
import org.example.company.tcs.techcellshop.messaging.OutboxPublisherJob;
import org.example.company.tcs.techcellshop.repository.OutboxEventRepository;
import org.example.company.tcs.techcellshop.util.OrderStatus;
import org.example.company.tcs.techcellshop.util.OutboxEventStatus;
import org.example.company.tcs.techcellshop.util.PaymentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OutboxPublisherJob")
class OutboxPublisherJobTest {

    @Mock private OutboxEventRepository outboxEventRepository;
    @Mock private RabbitTemplate rabbitTemplate;

    // Use real ObjectMapper so serialization/deserialization round-trips correctly
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    private OutboxPublisherJob job;

    private OutboxEvent pendingEvent;

    @BeforeEach
    void setUp() {
        job = new OutboxPublisherJob(
                outboxEventRepository,
                rabbitTemplate,
                objectMapper,
                "test.exchange",
                "test.routing-key",
                new SimpleMeterRegistry()
        );

        OrderCreatedEvent event = new OrderCreatedEvent(
                UUID.randomUUID().toString(),
                1L, 1L, 1L, 1,
                3999.90,
                "PIX",
                OrderStatus.CREATED,
                PaymentStatus.PENDING,
                Instant.now()
        );

        pendingEvent = new OutboxEvent();
        pendingEvent.setId(1L);
        pendingEvent.setEventId(event.eventId());
        pendingEvent.setEventType("order.created");
        pendingEvent.setAggregateType("Order");
        pendingEvent.setAggregateId(1L);
        pendingEvent.setStatus(OutboxEventStatus.PENDING);
        pendingEvent.setAttempts(0);
        pendingEvent.setNextAttemptAt(Instant.now().minusSeconds(1));
        pendingEvent.setCreatedAt(Instant.now().minusSeconds(5));

        try {
            pendingEvent.setPayload(objectMapper.writeValueAsString(event));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Nested
    @DisplayName("pollAndPublish")
    class PollAndPublish {

        @Test
        @DisplayName("should publish event and mark SENT on success")
        void shouldPublishAndMarkSent() {
            when(outboxEventRepository.findPendingBatch(
                    eq(OutboxEventStatus.PENDING), any(Instant.class), any(Pageable.class)))
                    .thenReturn(List.of(pendingEvent));
            when(outboxEventRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            job.pollAndPublishOutboxEvents();

            verify(rabbitTemplate).convertAndSend(
                    eq("test.exchange"), eq("test.routing-key"), any(OrderCreatedEvent.class));

            ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
            verify(outboxEventRepository).save(captor.capture());

            OutboxEvent saved = captor.getValue();
            assertThat(saved.getStatus()).isEqualTo(OutboxEventStatus.SENT);
            assertThat(saved.getSentAt()).isNotNull();
            assertThat(saved.getLastError()).isNull();
        }

        @Test
        @DisplayName("should increment attempts and schedule retry when RabbitMQ fails")
        void shouldRetry_whenRabbitFails() {
            when(outboxEventRepository.findPendingBatch(
                    eq(OutboxEventStatus.PENDING), any(Instant.class), any(Pageable.class)))
                    .thenReturn(List.of(pendingEvent));
            doThrow(new RuntimeException("broker unavailable"))
                    .when(rabbitTemplate).convertAndSend(any(), any(), any(Object.class));
            when(outboxEventRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            job.pollAndPublishOutboxEvents();

            ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
            verify(outboxEventRepository).save(captor.capture());

            OutboxEvent saved = captor.getValue();
            assertThat(saved.getStatus()).isEqualTo(OutboxEventStatus.PENDING);
            assertThat(saved.getAttempts()).isEqualTo(1);
            assertThat(saved.getLastError()).contains("broker unavailable");
            assertThat(saved.getNextAttemptAt()).isAfter(Instant.now());
        }

        @Test
        @DisplayName("should mark FAILED permanently after max attempts")
        void shouldMarkFailed_whenMaxAttemptsReached() {
            pendingEvent.setAttempts(4); // one more will hit MAX_ATTEMPTS = 5

            when(outboxEventRepository.findPendingBatch(
                    eq(OutboxEventStatus.PENDING), any(Instant.class), any(Pageable.class)))
                    .thenReturn(List.of(pendingEvent));
            doThrow(new RuntimeException("still down"))
                    .when(rabbitTemplate).convertAndSend(any(), any(), any(Object.class));
            when(outboxEventRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            job.pollAndPublishOutboxEvents();

            ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
            verify(outboxEventRepository).save(captor.capture());

            OutboxEvent saved = captor.getValue();
            assertThat(saved.getStatus()).isEqualTo(OutboxEventStatus.FAILED);
            assertThat(saved.getAttempts()).isEqualTo(5);
        }

        @Test
        @DisplayName("should do nothing when no pending events exist")
        void shouldDoNothing_whenNoPendingEvents() {
            when(outboxEventRepository.findPendingBatch(
                    eq(OutboxEventStatus.PENDING), any(Instant.class), any(Pageable.class)))
                    .thenReturn(List.of());

            job.pollAndPublishOutboxEvents();

            verify(rabbitTemplate, never()).convertAndSend(any(), any(), any(Object.class));
            verify(outboxEventRepository, never()).save(any());
        }

        @Test
        @DisplayName("should mark FAILED permanently when payload is corrupt JSON")
        void shouldMarkFailed_whenPayloadIsCorrupt() {
            pendingEvent.setPayload("not-valid-json{{{");

            when(outboxEventRepository.findPendingBatch(
                    eq(OutboxEventStatus.PENDING), any(Instant.class), any(Pageable.class)))
                    .thenReturn(List.of(pendingEvent));
            when(outboxEventRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            job.pollAndPublishOutboxEvents();

            ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
            verify(outboxEventRepository).save(captor.capture());

            OutboxEvent saved = captor.getValue();
            assertThat(saved.getStatus()).isEqualTo(OutboxEventStatus.FAILED);
        }
    }
}