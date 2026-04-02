package org.example.company.tcs.techcellshop.messaging;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
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

    private final Counter processedCounter;
    private final Counter duplicateCounter;
    private final Timer processingTimer;

    public OrderCreatedListener(ProcessedEventRepository processedEventRepository, MeterRegistry meterRegistry) {
        this.processedEventRepository = processedEventRepository;
        this.processedCounter = meterRegistry.counter("techcellshop.rabbit.order_created.processed");
        this.duplicateCounter = meterRegistry.counter("techcellshop.rabbit.order_created.duplicate");
        this.processingTimer = meterRegistry.timer("techcellshop.rabbit.order_created.duration");
    }

    @Transactional
    @RabbitListener(queues = "${app.messaging.order-created.queue}")
    public void handle(OrderCreatedEvent event) {
        processingTimer.record(() -> {
            if (processedEventRepository.existsByEventId(event.eventId())) {
                duplicateCounter.increment();
                log.info("Duplicate event received with eventId={}, ignoring", event.eventId());
                return;
            }

            log.info("Received order.created event for orderId={} userId={}", event.orderId(), event.userId());

            ProcessedEvent processedEvent = new ProcessedEvent();
            processedEvent.setEventId(event.eventId());
            processedEvent.setEventType("order.created");
            processedEvent.setProcessedAt(Instant.now());
            processedEventRepository.save(processedEvent);

            processedCounter.increment();
        });
    }
}
