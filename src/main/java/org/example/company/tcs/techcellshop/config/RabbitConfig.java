package org.example.company.tcs.techcellshop.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
@Configuration
public class RabbitConfig {

    @Bean
    TopicExchange orderExchange(@Value("${app.messaging.order-created.exchange}") String exchange) {
        return new TopicExchange(exchange, true, false);
    }

    @Bean
    DirectExchange dlx(@Value("${app.messaging.order-created.dlx}") String dlx) {
        return new DirectExchange(dlx, true, false);
    }

    @Bean
    Queue orderCreatedQueue(@Value("${app.messaging.order-created.queue}") String queue, @Value("${app.messaging.order-created.dlx}") String dlx, @Value("${app.messaging.order-created.dlq-routing-key}") String dlqRoutingKey) {
        return QueueBuilder.durable(queue)
                .withArgument("x-dead-letter-exchange", dlx)
                .withArgument("x-dead-letter-routing-key", dlqRoutingKey)
                .build();
    }

    @Bean
    Queue orderCreatedDlq(@Value("${app.messaging.order-created.dlq}") String dlq) {
        return QueueBuilder.durable(dlq).build();
    }

    @Bean
    Binding orderCreatedBinding(Queue orderCreatedQueue, TopicExchange orderExchange, @Value("${app.messaging.order-created.routing-key}") String routingKey) {
        return BindingBuilder.bind(orderCreatedQueue).to(orderExchange).with(routingKey);
    }

    @Bean
    Binding dlqBinding(Queue orderCreatedDlq, DirectExchange dlx, @Value("${app.messaging.order-created.dlq-routing-key}") String dlqRoutingKey) {
        return BindingBuilder.bind(orderCreatedDlq).to(dlx).with(dlqRoutingKey);
    }

    @Bean
    MessageConverter messageConverter() {
        return new JacksonJsonMessageConverter();
    }

    @Bean
    RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter);
        return rabbitTemplate;
    }


}
