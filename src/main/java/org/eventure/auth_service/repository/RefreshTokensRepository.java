package org.eventure.auth_service.repository;

import org.eventure.auth_service.model.entity.RefreshTokens;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RefreshTokensRepository extends JpaRepository<RefreshTokens, Long> {
}
