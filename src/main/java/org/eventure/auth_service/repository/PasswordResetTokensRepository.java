package org.eventure.auth_service.repository;

import org.eventure.auth_service.model.entity.PasswordResetTokens;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface PasswordResetTokensRepository extends JpaRepository<PasswordResetTokens, Long> {

    @Query("SELECT t FROM PasswordResetTokens t WHERE t.tokenHash = :hash AND t.expiresAt > :now AND t.usedAt IS NULL")
    Optional<PasswordResetTokens> findValidToken(@Param("hash") String tokenHash, LocalDateTime now);
}
