package org.eventure.auth_service.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eventure.auth_service.model.dto.RegisterRequestDto;
import org.eventure.auth_service.model.dto.UserRegistrationMessage;
import org.eventure.auth_service.model.entity.User;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserRegistrationPublisher {
    
    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.exchange.user-events}")
    private String userEventsExchange;

    @Value("${rabbitmq.routing-key.user-registration}")
    private String routingKey;

    public void publish(User user, RegisterRequestDto data) {
        UserRegistrationMessage message = new UserRegistrationMessage(
            user.getId(),
            data.getFirstName(),
            data.getLastName(),
            user.getEmail()
        );
        publishEvent(routingKey, message, user.getId());
    }

    private void publishEvent(String routingKey, Object message, Long userId) {
        try {
            rabbitTemplate.convertAndSend(userEventsExchange, routingKey, message);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send registration message", e);
        }
    }
}
