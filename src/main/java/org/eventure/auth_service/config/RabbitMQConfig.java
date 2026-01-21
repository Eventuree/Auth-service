package org.eventure.auth_service.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {
    
    @Value("${rabbitmq.exchange.user-events}")
    private String userEventsExchange;
    
    @Value("${rabbitmq.queue.user-registration}")
    private String userRegistrationQueue;
    
    @Value("${rabbitmq.routing-key.user-registration}")
    private String userRegistrationRoutingKey;

    @Value("${rabbitmq.queue.password-reset}")
    private String passwordResetQueue;

    @Value("${rabbitmq.routing-key.password-reset}")
    private String passwordResetRoutingKey;
    
    @Bean
    public TopicExchange userEventsExchange() {
        return new TopicExchange(userEventsExchange);
    }
    
    @Bean
    public Queue userRegistrationQueue() {
        return QueueBuilder.durable(userRegistrationQueue)
            .withArgument("x-dead-letter-exchange", userEventsExchange + ".dlx")
            .build();
    }
    
    @Bean
    public Binding userRegistrationBinding() {
        return BindingBuilder
            .bind(userRegistrationQueue())
            .to(userEventsExchange())
            .with(userRegistrationRoutingKey);
    }

    @Bean
    public Queue passwordResetQueue() {
        return QueueBuilder.durable(passwordResetQueue)
                .withArgument("x-dead-letter-exchange", "user.events.dlx")
                .build();
    }

    @Bean
    public Binding passwordResetBinding() {
        return BindingBuilder
                .bind(passwordResetQueue())
                .to(userEventsExchange())
                .with(passwordResetRoutingKey);
    }

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
    
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        return template;
    }
}
