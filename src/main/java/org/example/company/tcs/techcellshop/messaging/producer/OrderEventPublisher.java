package org.example.company.tcs.techcellshop.messaging.producer;

import org.example.company.tcs.techcellshop.messaging.OrderCanceledEvent;
import org.example.company.tcs.techcellshop.messaging.event.OrderCreatedEvent;
import org.example.company.tcs.techcellshop.messaging.event.PaymentConfirmedEvent;

public interface OrderEventPublisher {
    void publishOrderCreated(OrderCreatedEvent event);
    void publishPaymentConfirmed(PaymentConfirmedEvent event);
    void publishOrderCanceled(OrderCanceledEvent event);
}
