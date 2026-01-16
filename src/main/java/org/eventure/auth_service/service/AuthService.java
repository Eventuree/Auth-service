package org.eventure.auth_service.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eventure.auth_service.exception.*;
import org.eventure.auth_service.model.dto.AuthResponse;
import org.eventure.auth_service.model.dto.LoginRequestDto;
import org.eventure.auth_service.model.dto.RegisterRequestDto;
import org.eventure.auth_service.model.dto.UserRegistrationMessage;
import org.eventure.auth_service.model.entity.AuthCredentials;
import org.eventure.auth_service.model.entity.RefreshTokens;
import org.eventure.auth_service.model.entity.User;
import org.eventure.auth_service.model.enums.AccountStatus;
import org.eventure.auth_service.model.enums.AuthProvider;
import org.eventure.auth_service.model.enums.Role;
import org.eventure.auth_service.repository.AuthCredentialsRepository;
import org.eventure.auth_service.repository.RefreshTokensRepository;
import org.eventure.auth_service.repository.UserRepository;
import org.eventure.auth_service.security.JwtUtils;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final AuthCredentialsRepository authCredentialsRepository;
    private final RefreshTokensRepository refreshTokensRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.exchange.user-events}")
    private String userEventsExchange;

    @Value("${rabbitmq.routing-key.user-registration}")
    private String userRegistrationRoutingKey;

    @Value("${jwt.refresh-expiration}")
    private long refreshTokenExpiration;


    @Transactional
    public AuthResponse register(RegisterRequestDto request, HttpServletRequest httpRequest) {
        log.info("Attempting to register user with email: {}", request.getEmail());

        if (authCredentialsRepository.existsByEmail(request.getEmail())) {
            log.warn("Registration failed: email {} already exists", request.getEmail());
            throw new EmailAlreadyExistsException("User with email " + request.getEmail() + " already exists");
        }

        User user = User.builder()
                .email(request.getEmail())
                .role(Role.USER)
                .authProvider(AuthProvider.LOCAL)
                .build();

        User savedUser = userRepository.save(user);
        log.info("User created with ID: {}", savedUser.getId());

        AuthCredentials credentials = AuthCredentials.builder()
                .userId(savedUser.getId())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .accountStatus(AccountStatus.ACTIVE)
                .build();

        authCredentialsRepository.save(credentials);
        log.info("Auth credentials created for user ID: {}", savedUser.getId());

        sendUserRegistrationMessage(savedUser, request);

        String accessToken = jwtUtils.generateToken(savedUser.getEmail(), savedUser.getRole());
        String refreshToken = generateAndSaveRefreshToken(savedUser, httpRequest);

        log.info("User registered successfully: {}", savedUser.getEmail());

        return new AuthResponse(
                accessToken,
                refreshToken,
                savedUser.getId(),
                savedUser.getEmail(),
                savedUser.getRole().name()
        );
    }


    @Transactional
    public AuthResponse login(LoginRequestDto request, HttpServletRequest httpRequest) {
        log.info("Login attempt for email: {}", request.getEmail());

        AuthCredentials credentials = authCredentialsRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> {
                    log.warn("Login failed: email {} not found", request.getEmail());
                    return new InvalidCredentialsException("Invalid email or password");
                });

        if (!AccountStatus.ACTIVE.equals(credentials.getAccountStatus())) {
            log.warn("Login failed: account {} is not active", request.getEmail());
            throw new AccountInactiveException("Account is not active");
        }

        if (!passwordEncoder.matches(request.getPassword(), credentials.getPasswordHash())) {
            log.warn("Login failed: invalid password for email {}", request.getEmail());
            throw new InvalidCredentialsException("Invalid email or password");
        }

        User user = userRepository.findById(credentials.getUserId())
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        refreshTokensRepository.deleteAllByUserId(user.getId());
        log.info("Deleted all previous refresh tokens for user ID: {}", user.getId());

        String accessToken = jwtUtils.generateToken(user.getEmail(), user.getRole());
        String refreshToken = generateAndSaveRefreshToken(user, httpRequest);

        log.info("User logged in successfully: {}", user.getEmail());

        return new AuthResponse(
                accessToken,
                refreshToken,
                user.getId(),
                user.getEmail(),
                user.getRole().name()
        );
    }


    @Transactional
    public AuthResponse refreshToken(String refreshToken, HttpServletRequest httpRequest) {
        log.info("Refresh token request");

        String tokenHash = hashToken(refreshToken);

        RefreshTokens storedToken = refreshTokensRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> {
                    log.warn("Refresh token not found in database");
                    return new InvalidTokenException("Invalid refresh token");
                });

        if (storedToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            log.warn("Refresh token expired for user ID: {}", storedToken.getUserId());
            refreshTokensRepository.delete(storedToken);
            throw new InvalidTokenException("Refresh token expired");
        }

        User user = userRepository.findById(storedToken.getUserId())
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        if (!jwtUtils.isTokenValid(refreshToken, user.getEmail())) {
            log.warn("Refresh token validation failed for user: {}", user.getEmail());
            throw new InvalidTokenException("Invalid refresh token");
        }

        String newAccessToken = jwtUtils.generateToken(user.getEmail(), user.getRole());
        String newRefreshToken = generateAndSaveRefreshToken(user, httpRequest);

        refreshTokensRepository.delete(storedToken);

        log.info("Tokens refreshed for user: {}", user.getEmail());

        return new AuthResponse(
                newAccessToken,
                newRefreshToken,
                user.getId(),
                user.getEmail(),
                user.getRole().name()
        );
    }


    @Transactional
    public void logout(String refreshToken) {
        log.info("Logout request");

        String tokenHash = hashToken(refreshToken);

        RefreshTokens storedToken = refreshTokensRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new InvalidTokenException("Invalid refresh token"));

        refreshTokensRepository.delete(storedToken);

        log.info("User logged out, refresh token deleted for user ID: {}", storedToken.getUserId());
    }


    private String generateAndSaveRefreshToken(User user, HttpServletRequest httpRequest) {
        String refreshToken = jwtUtils.generateRefreshToken(user.getEmail(), user.getRole());

        String tokenHash = hashToken(refreshToken);

        String clientIp = getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        RefreshTokens tokenEntity = RefreshTokens.builder()
                .userId(user.getId())
                .tokenHash(tokenHash)
                .expiresAt(LocalDateTime.now().plusSeconds(refreshTokenExpiration / 1000))
                .clientIp(clientIp)
                .userAgent(userAgent)
                .build();

        refreshTokensRepository.save(tokenEntity);

        return refreshToken;
    }



    private void sendUserRegistrationMessage(User user, RegisterRequestDto request) {
        UserRegistrationMessage message = new UserRegistrationMessage(
                user.getId(),
                request.getFirstName(),
                request.getLastName(),
                user.getEmail()
        );

        try {
            rabbitTemplate.convertAndSend(
                    userEventsExchange,
                    userRegistrationRoutingKey,
                    message
            );
            log.info("User registration message sent to RabbitMQ for user ID: {}", user.getId());
        } catch (Exception e) {
            log.error("Failed to send user registration message to RabbitMQ for user ID: {}", user.getId(), e);
        }
    }


    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }


    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
