package com.example.jhapcham.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "app.rabbitmq", name = "enabled", havingValue = "true")
public class RabbitTopologyConfig {

    public static final String EXCHANGE = "jhapcham.domain";
    public static final String ORDER_EVENTS_QUEUE = "jhapcham.order-events";
    public static final String NOTIFICATION_QUEUE = "jhapcham.notifications";
    public static final String ANALYTICS_QUEUE = "jhapcham.analytics";
    public static final String SEARCH_INDEX_QUEUE = "jhapcham.search-index";

    @Bean
    DirectExchange domainExchange() {
        return new DirectExchange(EXCHANGE, true, false);
    }

    @Bean
    Queue orderEventsQueue() {
        return new Queue(ORDER_EVENTS_QUEUE, true);
    }

    @Bean
    Queue notificationQueue() {
        return new Queue(NOTIFICATION_QUEUE, true);
    }

    @Bean
    Queue analyticsQueue() {
        return new Queue(ANALYTICS_QUEUE, true);
    }

    @Bean
    Queue searchIndexQueue() {
        return new Queue(SEARCH_INDEX_QUEUE, true);
    }

    @Bean
    Binding orderEventsBinding(DirectExchange domainExchange, Queue orderEventsQueue) {
        return BindingBuilder.bind(orderEventsQueue).to(domainExchange).with("order.*");
    }

    @Bean
    Binding notificationBinding(DirectExchange domainExchange, Queue notificationQueue) {
        return BindingBuilder.bind(notificationQueue).to(domainExchange).with("notification.*");
    }

    @Bean
    Binding analyticsBinding(DirectExchange domainExchange, Queue analyticsQueue) {
        return BindingBuilder.bind(analyticsQueue).to(domainExchange).with("analytics.*");
    }

    @Bean
    Binding searchIndexBinding(DirectExchange domainExchange, Queue searchIndexQueue) {
        return BindingBuilder.bind(searchIndexQueue).to(domainExchange).with("search.*");
    }

    @Bean
    MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
