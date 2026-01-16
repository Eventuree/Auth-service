package org.eventure.auth_service.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.eventure.auth_service.model.dto.AuthResponse;
import org.eventure.auth_service.model.dto.LoginRequestDto;
import org.eventure.auth_service.model.dto.LogoutRequest;
import org.eventure.auth_service.model.dto.RegisterRequestDto;
import org.eventure.auth_service.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    
    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
        @Valid @RequestBody RegisterRequestDto request,
        HttpServletRequest httpRequest
    ) {
        AuthResponse response = authService.register(request, httpRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
        @Valid @RequestBody LoginRequestDto request,
        HttpServletRequest httpRequest
    ) {
        AuthResponse response = authService.login(request, httpRequest);
        return ResponseEntity.ok(response);
    }
    

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(
        @RequestHeader("Authorization") String refreshToken,
        HttpServletRequest httpRequest
    ) {
        if (refreshToken.startsWith("Bearer ")) {
            refreshToken = refreshToken.substring(7);
        }
        
        AuthResponse response = authService.refreshToken(refreshToken, httpRequest);
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
