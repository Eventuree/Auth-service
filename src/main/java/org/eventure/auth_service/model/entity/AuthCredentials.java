package org.eventure.auth_service.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "auth_credentials")
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AuthCredentials {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    private String email;

    @Column(name = "password_hash")
    private String passwordHash;

    @Column(name = "account_status")
    private String accountStatus;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

}
