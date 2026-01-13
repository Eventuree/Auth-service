package org.eventure.auth_service.service;

import lombok.RequiredArgsConstructor;
import org.eventure.auth_service.model.entity.SocialAuthIdentities;
import org.eventure.auth_service.model.entity.User;
import org.eventure.auth_service.repository.SocialAuthIdentitiesRepository;
import org.eventure.auth_service.repository.UserRepository;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.eventure.auth_service.model.enums.AuthProvider;

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
        String googleId = oAuth2User.getAttribute("sub");

        System.out.println("Google Login Email: " + email);

        User user = userRepository.findByEmail(email)
                .orElseGet(() -> registerNewUser(email));

        saveSocialIdentity(user, googleId);

        return oAuth2User;
    }

    private User registerNewUser(String email) {
        User newUser = User.builder()
                .email(email)
                .role("USER")
                .authProvider(AuthProvider.GOOGLE)
                .build();
        return userRepository.save(newUser);
    }

    private void saveSocialIdentity(User user, String providerUserId) {
        SocialAuthIdentities identity = SocialAuthIdentities.builder()
                .userId(user.getId())
                .providerName("GOOGLE")
                .providerUserId(providerUserId)
                .loginData("{}")
                .build();

        socialRepository.save(identity);
    }
}