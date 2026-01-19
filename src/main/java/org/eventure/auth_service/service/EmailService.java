package org.eventure.auth_service.service;

import java.util.Map;

public interface EmailService {
    void sendTemplateMail(String to, String subject, String templateName, Map<String, Object> variables);

    void sendPasswordResetMail(String userEmail, String rawToken);
}
