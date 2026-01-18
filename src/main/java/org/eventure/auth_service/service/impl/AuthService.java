package org.eventure.auth_service.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eventure.auth_service.exception.*;
import org.eventure.auth_service.messaging.UserRegistrationPublisher;
import org.eventure.auth_service.model.dto.AuthResponse;
import org.eventure.auth_service.model.dto.LoginRequestDto;
import org.eventure.auth_service.model.dto.RegisterRequestDto;
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
import org.eventure.auth_service.utills.TokenHashUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final AuthCredentialsRepository authCredentialsRepository;
    private final RefreshTokensRepository refreshTokensRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final UserRegistrationPublisher userRegistrationPublisher;

    @Value("${jwt.refresh-expiration}")
    private long refreshTokenExpiration;

    @Transactional
    public AuthResponse register(RegisterRequestDto request, String ipAddress, String userAgent) {

        if (authCredentialsRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException("User with email " + request.getEmail() + " already exists");
        }

        User user = User.builder()
                .email(request.getEmail())
                .role(Role.USER)
                .authProvider(AuthProvider.LOCAL)
                .build();

        User savedUser = userRepository.save(user);

        AuthCredentials credentials = AuthCredentials.builder()
                .userId(savedUser.getId())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .accountStatus(AccountStatus.ACTIVE)
                .build();

        authCredentialsRepository.save(credentials);

        String accessToken = jwtUtils.generateToken(savedUser.getEmail(), savedUser.getRole());
        String refreshToken = generateAndSaveRefreshToken(savedUser, ipAddress, userAgent);

        //userRegistrationPublisher.publish(savedUser, request);

        return new AuthResponse(
                accessToken,
                refreshToken,
                savedUser.getId(),
                savedUser.getEmail(),
                savedUser.getRole().name()
        );
    }


    @Transactional
    public AuthResponse login(LoginRequestDto request, String ipAddress, String userAgent) {

        AuthCredentials credentials = authCredentialsRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> {
                    return new InvalidCredentialsException("Invalid email or password");
                });

        if (!AccountStatus.ACTIVE.equals(credentials.getAccountStatus())) {
            throw new AccountInactiveException("Account is not active");
        }

        if (!passwordEncoder.matches(request.getPassword(), credentials.getPasswordHash())) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        User user = userRepository.findById(credentials.getUserId())
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        refreshTokensRepository.deleteAllByUserId(user.getId());

        String accessToken = jwtUtils.generateToken(user.getEmail(), user.getRole());
        String refreshToken = generateAndSaveRefreshToken(user, ipAddress, userAgent);


        return new AuthResponse(
                accessToken,
                refreshToken,
                user.getId(),
                user.getEmail(),
                user.getRole().name()
        );
    }


    @Transactional
    public AuthResponse refreshToken(String refreshToken, String ipAddress, String userAgent) {
        if (refreshToken.startsWith("Bearer ")) {
            refreshToken = refreshToken.substring(7);
        }
        String tokenHash = TokenHashUtil.hashToken(refreshToken);

        RefreshTokens storedToken = refreshTokensRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> {
                    return new InvalidTokenException("Invalid refresh token");
                });

        if (storedToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            refreshTokensRepository.delete(storedToken);
            throw new InvalidTokenException("Refresh token expired");
        }

        User user = userRepository.findById(storedToken.getUserId())
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        if (!jwtUtils.isTokenValid(refreshToken, user.getEmail())) {
            throw new InvalidTokenException("Invalid refresh token");
        }

        String newAccessToken = jwtUtils.generateToken(user.getEmail(), user.getRole());
        String newRefreshToken = generateAndSaveRefreshToken(user, ipAddress, userAgent);

        refreshTokensRepository.delete(storedToken);


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

        if (refreshToken.startsWith("Bearer ")) {
            refreshToken = refreshToken.substring(7);
        }

        String tokenHash = TokenHashUtil.hashToken(refreshToken);

        RefreshTokens storedToken = refreshTokensRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new InvalidTokenException("Invalid refresh token"));

        refreshTokensRepository.delete(storedToken);

    }


    private String generateAndSaveRefreshToken(User user, String clientIp, String userAgent) {
        String refreshToken = jwtUtils.generateRefreshToken(user.getEmail(), user.getRole());

        String tokenHash = TokenHashUtil.hashToken(refreshToken);

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

}
