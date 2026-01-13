package org.eventure.auth_service.repository;

import org.eventure.auth_service.model.entity.SocialAuthIdentities;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SocialAuthIdentitiesRepository extends JpaRepository<SocialAuthIdentities, Long> {
    Optional<SocialAuthIdentities> findByProviderUserId(String providerUserId);
}
