package org.eventure.auth_service.service;

import lombok.RequiredArgsConstructor;
import org.eventure.auth_service.model.entity.SocialAuthIdentities;
import org.eventure.auth_service.model.entity.User;
import org.eventure.auth_service.model.enums.AuthProvider;
import org.eventure.auth_service.model.enums.Role;
import org.eventure.auth_service.repository.SocialAuthIdentitiesRepository;
import org.eventure.auth_service.repository.UserRepository;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final SocialAuthIdentitiesRepository socialRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String email = oAuth2User.getAttribute("email");
        String providerId = oAuth2User.getAttribute("sub");
        String providerName = userRequest.getClientRegistration().getRegistrationId();

        Optional<SocialAuthIdentities> socialIdentity = socialRepository.findByProviderUserId(providerId);

        User user;
        if (socialIdentity.isPresent()) {
            Long userId = socialIdentity.get().getUserId();
            user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found but social identity exists"));
        } else {
            user = userRepository.findByEmail(email)
                    .orElseGet(() -> registerNewUser(email));

            saveSocialIdentity(user, providerId, providerName);
        }

        return oAuth2User;
    }

    private User registerNewUser(String email) {
        User user = User.builder()
                .email(email)
                .role(Role.USER)
                .authProvider(AuthProvider.GOOGLE)
                .build();
        return userRepository.save(user);
    }

    private void saveSocialIdentity(User user, String providerId, String providerName) {
        SocialAuthIdentities identity = SocialAuthIdentities.builder()
                .userId(user.getId())
                .providerName(providerName)
                .providerUserId(providerId)
                .loginData("{}")
                .build();
        socialRepository.save(identity);
    }
}