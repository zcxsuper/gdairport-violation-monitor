package com.example.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    @Bean
    public MessageConverter messageConverter() {
        Jackson2JsonMessageConverter jackson2JsonMessageConverter = new Jackson2JsonMessageConverter();
        jackson2JsonMessageConverter.setCreateMessageIds(true);
        return jackson2JsonMessageConverter;
    }


    public static final String MESSAGE_QUEUE = "chat-message-queue";
    public static final String MESSAGE_EXCHANGE = "chat-message-exchange";
    public static final String MESSAGE_ROUTING_KEY = "chat.message";

    @Bean
    public Queue chatMessageQueue() {
        return QueueBuilder.durable(MESSAGE_QUEUE)
                .lazy()
                .build();
    }

    @Bean
    public DirectExchange chatMessageExchange() {
        return new DirectExchange(MESSAGE_EXCHANGE);
    }

    @Bean
    public Binding chatMessageBinding(Queue chatMessageQueue, DirectExchange chatMessageExchange) {
        return BindingBuilder.bind(chatMessageQueue).to(chatMessageExchange).with(MESSAGE_ROUTING_KEY);
    }
}
