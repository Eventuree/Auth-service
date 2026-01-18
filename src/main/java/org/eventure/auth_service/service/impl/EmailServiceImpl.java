package org.eventure.auth_service.service.impl;

import org.eventure.auth_service.exception.EmailSendException;
import org.eventure.auth_service.service.EmailService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.password-reset.frontend-url}")
    private String resetBaseUrl;

    @Override
    public void sendPasswordResetMail(String userEmail, String rawToken) {
        String resetLink = resetBaseUrl + "?token=" + rawToken;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(userEmail);
        message.setSubject("Reset your Eventure password");

        message.setText("""
                Hello,

                We received a request to reset your Eventure account password.

                Click the link below to set a new password:
                %s

                This link will expire in 10 minutes.
                If you did not request this, you can safely ignore this email.

                Best regards,
                Eventure Team
                """.formatted(resetLink)
        );

        try {
            mailSender.send(message);
            log.info("Password reset email sent to {}", userEmail);
        } catch (Exception e) {
            log.error("Failed to send password reset email to {}", userEmail, e);
            throw new EmailSendException("Could not send password reset email");
        }
    }
}
