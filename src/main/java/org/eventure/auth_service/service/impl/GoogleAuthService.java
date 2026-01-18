package org.eventure.auth_service.service.impl;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eventure.auth_service.exception.InvalidTokenException;
import org.eventure.auth_service.model.dto.AuthResponse;
import org.eventure.auth_service.model.entity.User;
import org.eventure.auth_service.model.enums.AuthProvider;
import org.eventure.auth_service.model.enums.Role;
import org.eventure.auth_service.repository.UserRepository;
import org.eventure.auth_service.security.JwtUtils;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;

@Service
@RequiredArgsConstructor
@Slf4j
public class GoogleAuthService {

    private final GoogleIdTokenVerifier verifier;
    private final UserRepository userRepository;
    private final JwtUtils jwtUtils;

    public AuthResponse authenticate(String idTokenString) {
        GoogleIdToken.Payload payload = verifyGoogleToken(idTokenString);

        User user = findOrCreateUser(payload);

        String accessToken = jwtUtils.generateToken(user.getEmail(), user.getRole());
        String refreshToken = jwtUtils.generateRefreshToken(user.getEmail(), user.getRole());

        return new AuthResponse(
                accessToken,
                refreshToken,
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole().name()
        );
    }

    private GoogleIdToken.Payload verifyGoogleToken(String idTokenString) {
        try {
            GoogleIdToken idToken = verifier.verify(idTokenString);
            if (idToken == null) {
                throw new InvalidTokenException("Invalid Google ID Token");
            }
            return idToken.getPayload();
        } catch (GeneralSecurityException | IOException e) {
            log.error("Google token verification failed", e);
            throw new InvalidTokenException("Google token verification failed", e);
        }
    }

    private User findOrCreateUser(GoogleIdToken.Payload payload) {
        String email = payload.getEmail();
        return userRepository.findByEmail(email)
                .orElseGet(() -> {
                    String name = (String) payload.get("name");
                    return registerNewGoogleUser(email, name);
                });
    }

    private User registerNewGoogleUser(String email, String name) {
        User newUser = new User();
        newUser.setEmail(email);
        newUser.setFullName(name);
        newUser.setRole(Role.USER);
        newUser.setAuthProvider(AuthProvider.GOOGLE);
        return userRepository.save(newUser);
    }
}