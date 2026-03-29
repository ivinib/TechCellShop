package org.example.company.tcs.techcellshop.messaging;

import org.example.company.tcs.techcellshop.domain.ProcessedEvent;
import org.example.company.tcs.techcellshop.repository.ProcessedEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Component
public class OrderCreatedListener {

    private static final Logger log = LoggerFactory.getLogger(OrderCreatedListener.class);
    private final ProcessedEventRepository processedEventRepository;

    public OrderCreatedListener(ProcessedEventRepository processedEventRepository) {
        this.processedEventRepository = processedEventRepository;
    }

    @Transactional
    @RabbitListener(queues = "${app.messaging.order-created.queue}ueue")
    public void handle(OrderCreatedEvent event) {
        if (processedEventRepository.existsByEventId(event.eventId())) {
            log.info("Duplicate event received with eventId={}, ignoring", event.eventId());
            return;
        }

        log.info("Received order.created event for orderId={} userId={}", event.orderId(), event.userId());

        ProcessedEvent processedEvent = new ProcessedEvent();
        processedEvent.setEventId(event.eventId());
        processedEvent.setEventType("order.created");
        processedEvent.setProcessedAt(Instant.now());
        processedEventRepository.save(processedEvent);
    }
}
