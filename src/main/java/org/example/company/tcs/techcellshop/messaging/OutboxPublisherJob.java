package org.example.company.tcs.techcellshop.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.transaction.Transactional;
import org.example.company.tcs.techcellshop.domain.OutboxEvent;
import org.example.company.tcs.techcellshop.repository.OutboxEventRepository;
import org.example.company.tcs.techcellshop.util.OutboxEventStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
public class OutboxPublisherJob {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisherJob.class);
    private static final int BATCH_SIZE = 10;
    private static final int MAX_ATTEMPTS = 5;

    private final OutboxEventRepository outboxEventRepository;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    private final String exchange;
    private final String routingKey;

    private final Counter publishSuccessCounter;
    private final Counter publishFailureCounter;
    private final Counter publishPermanentFailureCounter;
    private final Timer publishTimer;

    public OutboxPublisherJob(OutboxEventRepository outboxEventRepository, RabbitTemplate rabbitTemplate, ObjectMapper objectMapper, @Value("${app.messaging.order-created.exchange}") String exchange, @Value("${app.messaging.order-created.routing-key}") String routingKey, MeterRegistry meterRegistry) {
        this.outboxEventRepository = outboxEventRepository;
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
        this.exchange = exchange;
        this.routingKey = routingKey;

        this.publishSuccessCounter = meterRegistry.counter("techcellshop.outbox.publish.success");
        this.publishFailureCounter = meterRegistry.counter("techcellshop.outbox.publish.failure");
        this.publishPermanentFailureCounter = meterRegistry.counter("techcellshop.outbox.publish.failed_permanently");
        this.publishTimer = meterRegistry.timer("techcellshop.outbox.publish.duration");
    }

    @Scheduled(fixedDelayString = "${app.outbox.poll-interval-ms:5000}")
    @Transactional
    public void pollAndPublishOutboxEvents() {
        List<OutboxEvent> pendingEvents = outboxEventRepository.findPendingBatch(OutboxEventStatus.PENDING, Instant.now(), PageRequest.of(0, BATCH_SIZE));

        if (pendingEvents.isEmpty()){
            log.info("No pending events found");
            return;
        }

        log.info("Outbox: Found {} pending event(s) to be published", pendingEvents.size());

        for (OutboxEvent event : pendingEvents) {
            processEvent(event);
        }
    }

    private void processEvent(OutboxEvent event) {
        publishTimer.record(() -> {
            try {
                OrderCreatedEvent payload = objectMapper.readValue(event.getPayload(), OrderCreatedEvent.class);
                rabbitTemplate.convertAndSend(exchange, routingKey, payload);

                event.setStatus(OutboxEventStatus.SENT);
                event.setSentAt(Instant.now());
                event.setLastError(null);

                outboxEventRepository.save(event);
                publishSuccessCounter.increment();

                log.info("Outbox: Successfully published event id={} type={}", event.getId(), event.getEventType());
            } catch (JsonProcessingException jsonProcessingException) {
                event.setAttempts(MAX_ATTEMPTS);
                event.setStatus(OutboxEventStatus.FAILED);
                event.setLastError("Failed to deserialize payload: " + jsonProcessingException.getMessage());

                outboxEventRepository.save(event);
                publishFailureCounter.increment();
                publishPermanentFailureCounter.increment();
                log.error("Outbox: corrupted payload: {}, for event id: {}, marking as FAILED",
                        jsonProcessingException.getMessage(), event.getId());
            }catch (Exception ex){
                int newAttempt = event.getAttempts() + 1;
                event.setAttempts(newAttempt);
                event.setLastError(ex.getMessage());

                publishFailureCounter.increment();

                if (newAttempt >= MAX_ATTEMPTS){
                    event.setStatus(OutboxEventStatus.FAILED);
                    publishPermanentFailureCounter.increment();
                    log.error("Outbox: Failed to publish event id={} type={}", event.getId(), event.getEventType());
                }else {
                    long backoffSeconds = (long) (Math.pow(2, newAttempt) * 5L);
                    event.setNextAttemptAt(Instant.now().plusSeconds(backoffSeconds));
                    log.warn("Outbox: Failed to publish event id={}, will retry at {}, attempt {}/{}", event.getId(), event.getNextAttemptAt(), newAttempt, MAX_ATTEMPTS);
                }
                outboxEventRepository.save(event);
            }
        });
    }

}
