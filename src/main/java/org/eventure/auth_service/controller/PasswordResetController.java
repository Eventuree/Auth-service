package org.eventure.auth_service.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.eventure.auth_service.model.dto.PasswordResetConfirmDto;
import org.eventure.auth_service.model.dto.PasswordResetDto;
import org.eventure.auth_service.model.dto.PasswordResetRequestDto;
import org.eventure.auth_service.service.PasswordResetService;
import org.eventure.auth_service.utills.HttpRequestUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth/password-reset")
@RequiredArgsConstructor
public class PasswordResetController {

    private final PasswordResetService passwordResetService;

    @PostMapping("/request")
    public ResponseEntity<String> requestPasswordReset(@RequestBody @Valid PasswordResetRequestDto resetDto,
                                                       HttpServletRequest http){
        String userEmail = resetDto.getEmail();

        passwordResetService.requestReset(userEmail,
                HttpRequestUtils.getClientIp(http),
                HttpRequestUtils.getUserAgent(http));

        return ResponseEntity.ok("Password reset link was successfully sent to email: " + resetDto.getEmail());
    }

    @PostMapping("/validate")
    public ResponseEntity<Void> validateResetToken(@RequestBody @Valid PasswordResetConfirmDto confirmDto){
        passwordResetService.validateResetToken(confirmDto.getToken());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/confirm")
    public ResponseEntity<Void> resetPassword(@RequestBody @Valid PasswordResetDto dto) {
        passwordResetService.resetPassword(dto.getToken(), dto.getNewPassword());
        return ResponseEntity.ok().build();
    }
}
