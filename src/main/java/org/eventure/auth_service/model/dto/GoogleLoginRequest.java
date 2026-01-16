package org.eventure.auth_service.model.dto;

import lombok.Data;

@Data
public class GoogleLoginRequest {
    private String idToken;
}