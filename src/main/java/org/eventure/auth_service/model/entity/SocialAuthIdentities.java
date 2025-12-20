package org.eventure.auth_service.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Builder
@Data
@Entity
@Table(name = "social_auth_identities")
@AllArgsConstructor
@NoArgsConstructor
public class SocialAuthIdentities {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "provider_name")
    private String providerName;

    @Column(name = "provider_user_id")
    private String providerUserId;

    @Column(name = "login_data", columnDefinition = "jsonb")
    private String loginData;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

}
