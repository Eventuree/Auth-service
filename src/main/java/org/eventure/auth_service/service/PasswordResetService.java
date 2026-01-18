package org.eventure.auth_service.service;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public interface PasswordResetService {
    void requestReset(String userEmail, String clientIp, String userAgent);

    void validateResetToken(@NotBlank String token);

    void resetPassword(@NotBlank String token, @NotBlank @Size(min = 8) String newPassword);
}
