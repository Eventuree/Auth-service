package org.eventure.auth_service.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eventure.auth_service.model.dto.PasswordResetEventDto;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.exchange.user-events}")
    private String userEventsExchange;

    @Value("${rabbitmq.routing-key.password-reset}")
    private String routingKey;

    public void publish(String email, String fullName, String token) {
        PasswordResetEventDto message = new PasswordResetEventDto(
                email,
                fullName,
                token
        );

        try {
            log.info("Publishing password reset event for: {}", email);
            rabbitTemplate.convertAndSend(userEventsExchange, routingKey, message);
        } catch (Exception e) {
            log.error("Failed to send password reset message", e);
        }
    }
}