package org.eventure.auth_service.controller;

import lombok.RequiredArgsConstructor;
import org.eventure.auth_service.model.dto.AuthResponse;
import org.eventure.auth_service.service.GoogleAuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final GoogleAuthService googleAuthService;

    @PostMapping("/google")
    public ResponseEntity<AuthResponse> googleLogin(@RequestBody Map<String, String> request) {
        String token = request.get("token");
        return ResponseEntity.ok(googleAuthService.authenticate(token));
    }
}