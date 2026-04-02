package org.example.company.tcs.techcellshop.messaging;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    public OutboxPublisherJob(OutboxEventRepository outboxEventRepository, RabbitTemplate rabbitTemplate, ObjectMapper objectMapper, @Value("${app.messaging.order-created.exchange}") String exchange, @Value("${app.messaging.order-created.routing-key}") String routingKey) {
        this.outboxEventRepository = outboxEventRepository;
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
        this.exchange = exchange;
        this.routingKey = routingKey;
    }

    @Scheduled(fixedDelayString = "${app.messaging.outbox-publisher-interval-ms:5000}")
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
        try{
            OrderCreatedDomainEvent payload = objectMapper.readValue(event.getPayload(), OrderCreatedDomainEvent.class);
            rabbitTemplate.convertAndSend(exchange, routingKey, payload);

            event.setStatus(OutboxEventStatus.SENT);
            event.setSentAt(Instant.now());
            event.setLastError(null);

            outboxEventRepository.save(event);

            log.info("Outbox: Successfully published event id={} type={}", event.getId(), event.getEventType());
        }catch (JsonMappingException jsonMappingException){
            event.setAttempts(MAX_ATTEMPTS);
            event.setStatus(OutboxEventStatus.FAILED);
            event.setLastError("Failed to deserialize payload: " + jsonMappingException.getMessage());
            outboxEventRepository.save(event);
            log.error("Outbox: corrupted payload: {}, for event id: {}, marking as FAILED ", jsonMappingException.getMessage(), event.getId());
        }catch (Exception ex){
            int newAttempt = event.getAttempts() + 1;
            event.setAttempts(newAttempt);
            event.setLastError(ex.getMessage());

            if (newAttempt >= MAX_ATTEMPTS){
                event.setStatus(OutboxEventStatus.FAILED);
                log.error("Outbox: Failed to publish event id={} type={}", event.getId(), event.getEventType());
            }else {
                long backoffSeconds = (long) (Math.pow(2, newAttempt) * 5L);
                event.setNextAttemptAt(Instant.now().plusSeconds(backoffSeconds));
                log.warn("Outbox: Failed to publish event id={}, will retry at {}, attempt {}/{}", event.getId(), event.getNextAttemptAt(), newAttempt, MAX_ATTEMPTS);
            }
            outboxEventRepository.save(event);
        }
    }

}
