package org.eventure.auth_service.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.eventure.auth_service.model.dto.*;
import org.eventure.auth_service.service.impl.AuthService;
import org.eventure.auth_service.utills.HttpRequestUtils;
import org.springframework.http.HttpStatus;
import org.eventure.auth_service.service.impl.GoogleAuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final GoogleAuthService googleAuthService;


    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterRequestDto request,
            HttpServletRequest httpRequest) {

        String ipAddress = HttpRequestUtils.getClientIp(httpRequest);
        String userAgent = HttpRequestUtils.getUserAgent(httpRequest);

        AuthResponse response = authService.register(request, ipAddress, userAgent);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/google/login")
    public ResponseEntity<AuthResponse> googleLogin(@RequestBody GoogleLoginRequest request) {
        return ResponseEntity.ok(googleAuthService.authenticate(request.getIdToken()));
    }


    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
        @Valid @RequestBody LoginRequestDto request,
        HttpServletRequest httpRequest
    ) {

        String ipAddress = HttpRequestUtils.getClientIp(httpRequest);
        String userAgent = HttpRequestUtils.getUserAgent(httpRequest);

        AuthResponse response = authService.login(request, ipAddress, userAgent);
        return ResponseEntity.ok(response);
    }


    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(
        @RequestHeader("Authorization") String refreshToken,
        HttpServletRequest httpRequest
    ) {

        String ipAddress = HttpRequestUtils.getClientIp(httpRequest);
        String userAgent = HttpRequestUtils.getUserAgent(httpRequest);

        AuthResponse response = authService.refreshToken(refreshToken, ipAddress, userAgent);
        return ResponseEntity.ok(response);
    }


    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(
            @RequestBody LogoutRequest request
    ) {

        authService.logout(request.getRefreshToken());
        return ResponseEntity.ok(Map.of(
                "message", "Logged out successfully"
        ));
    }
}
