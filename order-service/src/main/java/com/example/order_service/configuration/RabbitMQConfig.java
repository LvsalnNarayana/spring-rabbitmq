package com.example.order_service.configuration;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    /* =========================================================
       EXCHANGE NAMES
       ========================================================= */

    public static final String ORDER_STATUS_TOPIC_EXCHANGE =
            "order.status.topic.exchange";

    public static final String ORDER_ANALYTICS_FANOUT_EXCHANGE =
            "order.analytics.fanout.exchange";

    public static final String PAYMENT_STATUS_TOPIC_EXCHANGE =
            "payment.status.topic.exchange";

    /* =========================================================
       QUEUE NAMES (FOR DEMO / LOCAL / MOCK PURPOSES)
       ========================================================= */

    // Consumer simulation queues (normally owned by other services)
    public static final String ORDER_STATUS_QUEUE =
            "order.status.queue";

    public static final String ORDER_ANALYTICS_QUEUE =
            "order.analytics.queue";

    public static final String PAYMENT_STATUS_QUEUE =
            "payment.status.queue";

    /* =========================================================
       EXCHANGES
       ========================================================= */

    @Bean
    public TopicExchange orderStatusExchange() {
        return new TopicExchange(ORDER_STATUS_TOPIC_EXCHANGE, true, false);
    }

    @Bean
    public FanoutExchange orderAnalyticsExchange() {
        return new FanoutExchange(ORDER_ANALYTICS_FANOUT_EXCHANGE, true, false);
    }

    @Bean
    public TopicExchange paymentStatusExchange() {
        return new TopicExchange(PAYMENT_STATUS_TOPIC_EXCHANGE, true, false);
    }

    /* =========================================================
       QUEUES
       ========================================================= */

    /**
     * Simulates consumers like: - product-service - shipment-service
     */
    @Bean
    public Queue orderStatusQueue() {
        return QueueBuilder.durable(ORDER_STATUS_QUEUE).build();
    }

    /**
     * Analytics / monitoring consumer
     */
    @Bean
    public Queue orderAnalyticsQueue() {
        return QueueBuilder.durable(ORDER_ANALYTICS_QUEUE).build();
    }

    /**
     * Simulates payment-service consumer
     */
    @Bean
    public Queue paymentStatusQueue() {
        return QueueBuilder.durable(PAYMENT_STATUS_QUEUE).build();
    }

    /* =========================================================
       BINDINGS
       ========================================================= */

    /**
     * Order lifecycle events
     * <p>
     * Examples: - order.status.created - order.status.confirmed - order.status.cancelled
     */
    @Bean
    public Binding orderStatusBinding() {
        return BindingBuilder
                .bind(orderStatusQueue())
                .to(orderStatusExchange())
                .with("order.status.*");
    }

    /**
     * Fanout analytics binding
     */
    @Bean
    public Binding orderAnalyticsBinding() {
        return BindingBuilder
                .bind(orderAnalyticsQueue())
                .to(orderAnalyticsExchange());
    }

    /**
     * Payment-related events
     * <p>
     * Examples: - payment.status.initiated - payment.status.failed - payment.status.completed
     */
    @Bean
    public Binding paymentStatusBinding() {
        return BindingBuilder
                .bind(paymentStatusQueue())
                .to(paymentStatusExchange())
                .with("payment.status.*");
    }
}
