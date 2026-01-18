package org.eventure.auth_service.repository;

import org.eventure.auth_service.model.entity.AuthCredentials;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AuthCredentialsRepository extends JpaRepository<AuthCredentials, Long> {
    Optional<AuthCredentials> findByEmail(String email);

    boolean existsByEmail(String email);
}
