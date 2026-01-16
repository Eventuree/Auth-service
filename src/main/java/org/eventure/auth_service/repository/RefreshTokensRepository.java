package org.eventure.auth_service.repository;

import org.eventure.auth_service.model.entity.RefreshTokens;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface RefreshTokensRepository extends JpaRepository<RefreshTokens, Long> {
    Optional<RefreshTokens> findByTokenHash(String tokenHash);

    void deleteAllByUserId(Long userId);

}
