package org.eventure.auth_service.service.impl;

import jakarta.mail.internet.MimeMessage;
import org.eventure.auth_service.exception.EmailSendException;
import org.eventure.auth_service.service.EmailService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Map;


@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.password-reset.frontend-url}")
    private String resetBaseUrl;

    @Value("${app.password-reset.mail.subject}")
    private String resetPasswordSubject;

    @Value("${app.password-reset.template-name}")
    private String resetPasswordTemplateName;


    @Override
    public void sendPasswordResetMail(String userEmail, String rawToken) {
        String resetLink = resetBaseUrl + "?token=" + rawToken;

        Map<String, Object> vars = Map.of(
                "resetLink", resetLink
        );

        sendTemplateMail(
                userEmail,
                resetPasswordSubject,
                resetPasswordTemplateName,
                vars
        );
    }

    @Override
    public void sendTemplateMail(String to,
                                 String subject,
                                 String templateName,
                                 Map<String, Object> variables) {
        try {
            Context context = new Context();
            context.setVariables(variables);

            String htmlTemplate = "mail/" + templateName + ".html";
            String textTemplate = "mail/" + templateName + ".txt";

            String htmlBody = templateEngine.process(htmlTemplate, context);
            String textBody = templateEngine.process(textTemplate, context);

            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(textBody, htmlBody);

            mailSender.send(mimeMessage);
            log.info("Email '{}' sent to {}", subject, to);

        } catch (Exception e) {
            log.error("Failed to send email '{}' to {}", subject, to, e);
            throw new EmailSendException("Could not send email", e);
        }
    }
}
