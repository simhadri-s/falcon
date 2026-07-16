package com.laserxprts.falcon.service;

import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import com.laserxprts.falcon.repository.UserRepository;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService{
    
    private final UserRepository adminRepository;

    public CustomOAuth2UserService(UserRepository adminRepository) {
        this.adminRepository = adminRepository;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User googleUser = super.loadUser(userRequest);
        String email = googleUser.getAttribute("email");

        return adminRepository.findByEmail(email)
            .map(dbUser -> new CustomOAuth2User(dbUser, googleUser.getAttributes()))
            .orElseThrow(() -> new OAuth2AuthenticationException("Email not recognized. Traditional account must exist first."));
    }
}
