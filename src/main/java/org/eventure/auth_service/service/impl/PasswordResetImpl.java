package org.eventure.auth_service.service.impl;

import lombok.RequiredArgsConstructor;
import org.eventure.auth_service.exception.CredentialsNotFoundException;
import org.eventure.auth_service.exception.InvalidTokenException;
import org.eventure.auth_service.exception.UserNotFoundException;
import org.eventure.auth_service.exception.WrongAuthProviderException;
import org.eventure.auth_service.model.entity.AuthCredentials;
import org.eventure.auth_service.model.entity.PasswordResetTokens;
import org.eventure.auth_service.model.entity.User;
import org.eventure.auth_service.model.enums.AuthProvider;
import org.eventure.auth_service.repository.AuthCredentialsRepository;
import org.eventure.auth_service.repository.PasswordResetTokensRepository;
import org.eventure.auth_service.repository.RefreshTokensRepository;
import org.eventure.auth_service.repository.UserRepository;
import org.eventure.auth_service.service.EmailService;
import org.eventure.auth_service.service.PasswordResetService;
import org.eventure.auth_service.utills.TokenGeneratorUtil;
import org.eventure.auth_service.utills.TokenHashUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PasswordResetImpl implements PasswordResetService {

    private final PasswordResetTokensRepository passwordResetRepository;
    private final UserRepository userRepository;
    private final AuthCredentialsRepository authCredentialsRepository;
    private final RefreshTokensRepository refreshTokensRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.password-reset.timeout}")
    private long tokenExpirationTime;


    @Override
    public void requestReset(String userEmail, String clientIp, String userAgent) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UserNotFoundException("User with email " + userEmail + " not found"));

        if (user.getAuthProvider() != AuthProvider.LOCAL){
            throw new WrongAuthProviderException("User registered via external auth provider cannot reset the password!");
        }

        String rawToken = TokenGeneratorUtil.generateSecureToken();

        emailService.sendPasswordResetMail(userEmail, rawToken);

        String hashedToken = TokenHashUtil.hashToken(rawToken);

        LocalDateTime currentTime = LocalDateTime.now();

        PasswordResetTokens tokenEntity = PasswordResetTokens.builder()
                .userId(user.getId())
                .tokenHash(hashedToken)
                .issuedAt(currentTime)
                .expiresAt(currentTime.plusSeconds(tokenExpirationTime/1000))
                .clientIp(clientIp)
                .userAgent(userAgent)
                .build();

        passwordResetRepository.save(tokenEntity);
    }

    @Override
    public void validateResetToken(String rawToken) {
        String tokenHash = TokenHashUtil.hashToken(rawToken);

        passwordResetRepository.findValidToken(
                tokenHash,
                LocalDateTime.now()
        ).orElseThrow(() -> new InvalidTokenException("Token invalid or expired"));
    }

    @Transactional
    @Override
    public void resetPassword(String rawToken, String newPassword) {
        String tokenHash = TokenHashUtil.hashToken(rawToken);

        PasswordResetTokens token = passwordResetRepository.findValidToken(tokenHash, LocalDateTime.now())
                .orElseThrow(() -> new InvalidTokenException("Token invalid or expired"));

        AuthCredentials creds = authCredentialsRepository.findByUserId(token.getUserId())
                .orElseThrow(() -> new CredentialsNotFoundException("User credentials not found"));

        creds.setPasswordHash(passwordEncoder.encode(newPassword));
        authCredentialsRepository.save(creds);

        token.setUsedAt(LocalDateTime.now());
        passwordResetRepository.save(token);

        refreshTokensRepository.deleteAllByUserId(token.getUserId());
    }
}
