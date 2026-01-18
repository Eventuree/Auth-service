package org.eventure.auth_service.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserRegistrationMessage {
    private Long userId;
    private String firstName;
    private String lastName;
    private String email;
}
