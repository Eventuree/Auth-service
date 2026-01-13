package org.eventure.auth_service.controller;

import lombok.RequiredArgsConstructor;
import org.eventure.auth_service.security.JwtUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
public class TestController {

    private final JwtUtils jwtUtils;

    @GetMapping("/me")
    public String getMyInfo(@RequestHeader("Authorization") String tokenHeader) {

        String token = tokenHeader.substring(7);

        if (jwtUtils.isTokenValid(token, jwtUtils.extractEmail(token))) {
            return "Привіт! Твій email у токені: " + jwtUtils.extractEmail(token);
        } else {
            return "Токен недійсний!";
        }
    }
}