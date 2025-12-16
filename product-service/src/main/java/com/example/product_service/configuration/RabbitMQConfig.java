package com.example.product_service.configuration;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
public class RabbitMQConfig {

    /* =========================
       EXCHANGE NAMES
       ========================= */
    public static final String PRODUCTS_WAREHOUSE_TOPIC_EXCHANGE =
            "products.warehouse.topic.exchange";

    public static final String PRODUCTS_ANALYTICS_FANOUT_EXCHANGE =
            "products.analytics.fanout.exchange";

    public static final String PRODUCTS_NOTIFICATION_TTL_EXCHANGE =
            "products.notification.ttl.exchange";

    public static final String PRODUCTS_NOTIFICATION_USER_EXCHANGE =
            "products.notification.user.exchange";

    /* =========================
       QUEUE NAMES
       ========================= */

    // Analytics
    public static final String PRODUCTS_ANALYTICS_QUEUE =
            "products.analytics.queue";

    // Example warehouse queues (create per warehouse)
    public static final String PRODUCTS_WAREHOUSE_BLRA_QUEUE =
            "products.warehouse.BLRA.queue";

    // Notification
    public static final String PRODUCTS_NOTIFICATION_DELAY_QUEUE =
            "products.notification.delay.queue";

    public static final String PRODUCTS_NOTIFICATION_USER_QUEUE =
            "products.notification.user.queue";
//
//    @Bean
//    public ApplicationRunner forceRabbitAdminInit(RabbitAdmin rabbitAdmin) {
//        return args -> {
//            rabbitAdmin.initialize();
//            System.out.println(">>> RabbitAdmin initialized <<<");
//        };
//    }

    /* =========================
       EXCHANGES
       ========================= */

    @Bean
    public TopicExchange productsWarehouseExchange() {
        return new TopicExchange(PRODUCTS_WAREHOUSE_TOPIC_EXCHANGE);
    }

    @Bean
    public FanoutExchange productsAnalyticsExchange() {
        return new FanoutExchange(PRODUCTS_ANALYTICS_FANOUT_EXCHANGE);
    }

    @Bean
    public DirectExchange productsNotificationUserExchange() {
        return new DirectExchange(PRODUCTS_NOTIFICATION_USER_EXCHANGE);
    }

    @Bean
    public DirectExchange productsNotificationTtlExchange() {
        return new DirectExchange(PRODUCTS_NOTIFICATION_TTL_EXCHANGE);
    }

    /* =========================
       QUEUES
       ========================= */

    // Analytics queue (fanout → competing consumers)
    @Bean
    public Queue productsAnalyticsQueue() {
        return QueueBuilder.durable(PRODUCTS_ANALYTICS_QUEUE).build();
    }

    // Example warehouse queue (bind per warehouse)
    @Bean
    public Queue productsWarehouseBlraQueue() {
        return QueueBuilder.durable(PRODUCTS_WAREHOUSE_BLRA_QUEUE).build();
    }

    // Delay queue (1 hour TTL → DLX)
    @Bean
    public Queue productsNotificationDelayQueue() {
        return QueueBuilder.durable(PRODUCTS_NOTIFICATION_DELAY_QUEUE)
                .withArguments(Map.of(
                        "x-message-ttl", 60 * 1000, // 1 hour
                        "x-dead-letter-exchange", PRODUCTS_NOTIFICATION_USER_EXCHANGE,
                        "x-dead-letter-routing-key", "notify.user"
                ))
                .build();
    }

    // Final user notification queue
    @Bean
    public Queue productsNotificationUserQueue() {
        return QueueBuilder.durable(PRODUCTS_NOTIFICATION_USER_QUEUE).build();
    }

    /* =========================
       BINDINGS
       ========================= */

    // Analytics fanout binding
    @Bean
    public Binding analyticsBinding() {
        return BindingBuilder
                .bind(productsAnalyticsQueue())
                .to(productsAnalyticsExchange());
    }

    // Warehouse binding (BLRA example)
    @Bean
    public Binding warehouseBlraBinding() {
        return BindingBuilder
                .bind(productsWarehouseBlraQueue())
                .to(productsWarehouseExchange())
                .with("products.warehouse.*.queue");
    }

    // TTL exchange → delay queue
    @Bean
    public Binding notificationDelayBinding() {
        return BindingBuilder
                .bind(productsNotificationDelayQueue())
                .to(productsNotificationTtlExchange())
                .with("notify.delay");
    }

    // DLX → user notification queue
    @Bean
    public Binding notificationUserBinding() {
        return BindingBuilder
                .bind(productsNotificationUserQueue())
                .to(productsNotificationUserExchange())
                .with("notify.user");
    }
}
