package org.example.company.tcs.techcellshop.service.impl.order;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.company.tcs.techcellshop.domain.Order;
import org.example.company.tcs.techcellshop.domain.OutboxEvent;
import org.example.company.tcs.techcellshop.messaging.OrderCreatedEvent;
import org.example.company.tcs.techcellshop.repository.OutboxEventRepository;
import org.example.company.tcs.techcellshop.util.OutboxEventStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
public class OrderOutboxWriter {

    private static final Logger log = LoggerFactory.getLogger(OrderOutboxWriter.class);

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public OrderOutboxWriter(OutboxEventRepository outboxEventRepository, ObjectMapper objectMapper) {
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    public void writeOrderCreated(Order order) {
        OrderCreatedEvent event = new OrderCreatedEvent(
                UUID.randomUUID().toString(),
                order.getIdOrder(),
                order.getUser().getIdUser(),
                order.getDevice().getIdDevice(),
                order.getQuantityOrder(),
                order.getTotalPriceOrder(),
                order.getPaymentMethod(),
                order.getStatus(),
                order.getPaymentStatus(),
                Instant.now()
        );

        try {
            OutboxEvent outboxEvent = new OutboxEvent();
            outboxEvent.setEventId(event.eventId());
            outboxEvent.setEventType("order.created");
            outboxEvent.setAggregateType("Order");
            outboxEvent.setAggregateId(order.getIdOrder());
            outboxEvent.setPayload(objectMapper.writeValueAsString(event));
            outboxEvent.setStatus(OutboxEventStatus.PENDING);
            outboxEvent.setAttempts(0);
            outboxEvent.setNextAttemptAt(Instant.now());
            outboxEvent.setCreatedAt(Instant.now());

            outboxEventRepository.save(outboxEvent);
            log.info("Outbox event written for orderId={}", order.getIdOrder());
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException(
                    "Failed to serialize outbox event for orderId=" + order.getIdOrder(),
                    ex
            );
        }
    }
}
