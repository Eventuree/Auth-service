package org.eventure.auth_service.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import lombok.RequiredArgsConstructor;
import org.eventure.auth_service.model.dto.AuthResponse;
import org.eventure.auth_service.model.entity.User;
import org.eventure.auth_service.model.enums.AuthProvider;
import org.eventure.auth_service.model.enums.Role;
import org.eventure.auth_service.repository.UserRepository;
import org.eventure.auth_service.security.JwtUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;

@Service
@RequiredArgsConstructor
public class GoogleAuthService {

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String clientId;

    private final UserRepository userRepository;
    private final JwtUtils jwtUtils;

    @Transactional
    public AuthResponse authenticate(String idTokenString) {
        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
                    .setAudience(Collections.singletonList(clientId))
                    .build();

            GoogleIdToken idToken = verifier.verify(idTokenString);

            if (idToken == null) {
                throw new IllegalArgumentException("Invalid Google ID Token");
            }

            GoogleIdToken.Payload payload = idToken.getPayload();
            String email = payload.getEmail();
            String name = (String) payload.get("name");

            User user = userRepository.findByEmail(email)
                    .orElseGet(() -> registerNewGoogleUser(email, name));

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

        } catch (GeneralSecurityException | IOException e) {
            throw new RuntimeException("Error verifying google token", e);
        }
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