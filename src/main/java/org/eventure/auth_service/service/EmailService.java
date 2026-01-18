package org.eventure.auth_service.service;

public interface EmailService {
    void sendPasswordResetMail(String userEmail, String rawToken);
}
