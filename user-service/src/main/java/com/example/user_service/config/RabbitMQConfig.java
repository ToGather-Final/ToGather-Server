package com.example.user_service.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // Exchange 정의
    @Bean
    public TopicExchange userExchange() {
        return new TopicExchange("user.exchange");
    }

    @Bean
    public TopicExchange tradingExchange() {
        return new TopicExchange("trading.exchange");
    }

    @Bean
    public TopicExchange paymentExchange() {
        return new TopicExchange("payment.exchange");
    }

    @Bean
    public TopicExchange voteExchange() {
        return new TopicExchange("vote.exchange");
    }

    // Queue 정의
    @Bean
    public Queue userCreatedQueue() {
        return QueueBuilder.durable("user.created.queue").build();
    }

    @Bean
    public Queue userUpdatedQueue() {
        return QueueBuilder.durable("user.updated.queue").build();
    }

    @Bean
    public Queue userDeletedQueue() {
        return QueueBuilder.durable("user.deleted.queue").build();
    }

    @Bean
    public Queue tradingUserQueue() {
        return QueueBuilder.durable("trading.user.queue").build();
    }

    @Bean
    public Queue paymentUserQueue() {
        return QueueBuilder.durable("payment.user.queue").build();
    }

    @Bean
    public Queue voteUserQueue() {
        return QueueBuilder.durable("vote.user.queue").build();
    }

    // Binding 정의
    @Bean
    public Binding userCreatedBinding() {
        return BindingBuilder.bind(userCreatedQueue())
                .to(userExchange())
                .with("user.created");
    }

    @Bean
    public Binding userUpdatedBinding() {
        return BindingBuilder.bind(userUpdatedQueue())
                .to(userExchange())
                .with("user.updated");
    }

    @Bean
    public Binding userDeletedBinding() {
        return BindingBuilder.bind(userDeletedQueue())
                .to(userExchange())
                .with("user.deleted");
    }

    @Bean
    public Binding tradingUserBinding() {
        return BindingBuilder.bind(tradingUserQueue())
                .to(tradingExchange())
                .with("trading.user.*");
    }

    @Bean
    public Binding paymentUserBinding() {
        return BindingBuilder.bind(paymentUserQueue())
                .to(paymentExchange())
                .with("payment.user.*");
    }

    @Bean
    public Binding voteUserBinding() {
        return BindingBuilder.bind(voteUserQueue())
                .to(voteExchange())
                .with("vote.user.*");
    }
}
