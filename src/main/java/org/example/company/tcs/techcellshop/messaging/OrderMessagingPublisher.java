package org.example.company.tcs.techcellshop.messaging;

import org.example.company.tcs.techcellshop.domain.Order;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Instant;
import java.util.UUID;

@Component
public class OrderMessagingPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final String exchange;
    private final String routingKey;

    public OrderMessagingPublisher(RabbitTemplate rabbitTemplate, @Value("${app.messaging.order-created.exchange}") String exchange, @Value("${app.messaging.order-created.routing-key}") String routingKey) {
        this.rabbitTemplate = rabbitTemplate;
        this.exchange = exchange;
        this.routingKey = routingKey;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void orderCreated(OrderCreatedDomainEvent orderCreatedDomainEvent){
        Order order = orderCreatedDomainEvent.order();
        OrderCreatedEvent payload = new OrderCreatedEvent(
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
        rabbitTemplate.convertAndSend(exchange, routingKey, payload);
    }
}
