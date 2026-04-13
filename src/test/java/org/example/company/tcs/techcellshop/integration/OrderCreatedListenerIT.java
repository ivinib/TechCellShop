package org.example.company.tcs.techcellshop.integration;

import org.example.company.tcs.techcellshop.messaging.OrderCreatedEvent;
import org.example.company.tcs.techcellshop.repository.ProcessedEventRepository;
import org.example.company.tcs.techcellshop.util.OrderStatus;
import org.example.company.tcs.techcellshop.util.PaymentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@ActiveProfiles("it")
@DisplayName("Order Created Listener Integration Tests")
class OrderCreatedListenerIT extends AbstractMultiContainerIT {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private ProcessedEventRepository processedEventRepository;

    @BeforeEach
    void cleanUp() {
        processedEventRepository.deleteAll();
    }

    @Test
    @DisplayName("Should process same event only once when duplicate message is received")
    void shouldProcessSameEventOnlyOnceWhenDuplicateMessageIsReceived() {
        String eventId = UUID.randomUUID().toString();

        OrderCreatedEvent event = new OrderCreatedEvent(
                eventId,
                101L,
                201L,
                301L,
                1,
                new BigDecimal("3999.90"),
                "PIX",
                OrderStatus.CREATED,
                PaymentStatus.PENDING,
                Instant.now()
        );

        rabbitTemplate.convertAndSend("techcellshop.order.exchange", "order.created", event);
        rabbitTemplate.convertAndSend("techcellshop.order.exchange", "order.created", event);

        await()
                .atMost(15, TimeUnit.SECONDS)
                .pollInterval(1, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    assertThat(processedEventRepository.count()).isEqualTo(1);
                    assertThat(processedEventRepository.existsByEventId(eventId)).isTrue();
                });
    }

    @Test
    @DisplayName("Should persist processed event when a valid order.created message is consumed")
    void shouldPersistProcessedEventWhenValidMessageIsConsumed() {
        String eventId = UUID.randomUUID().toString();

        OrderCreatedEvent event = new OrderCreatedEvent(
                eventId,
                102L,
                202L,
                302L,
                2,
                new BigDecimal("7999.80"),
                "CREDIT_CARD",
                OrderStatus.CREATED,
                PaymentStatus.PENDING,
                Instant.now()
        );

        rabbitTemplate.convertAndSend("techcellshop.order.exchange", "order.created", event);

        await()
                .atMost(15, TimeUnit.SECONDS)
                .pollInterval(1, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    assertThat(processedEventRepository.count()).isEqualTo(1);
                    assertThat(processedEventRepository.existsByEventId(eventId)).isTrue();
                });
    }
}