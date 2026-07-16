package com.laserxprts.falcon.security;

import com.laserxprts.falcon.repository.UserRepository;
import com.laserxprts.falcon.service.CustomOAuth2User;
import com.laserxprts.falcon.model.User;
import com.laserxprts.falcon.utils.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtUtils jwtUtils;
    private final UserRepository userRepository;

    public OAuth2LoginSuccessHandler(JwtUtils jwtUtils, UserRepository userRepository) {
        this.jwtUtils = jwtUtils;
        this.userRepository = userRepository;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, 
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        CustomOAuth2User principal = (CustomOAuth2User) authentication.getPrincipal();

        User user = userRepository.findByEmail(principal.getName())
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        
        // 1. Generate the JWT Token
        String token = jwtUtils.generateToken(principal.getName(), user.getRoles(), user.getTokenVersion());

        // 2. Define the React frontend URL and attach the token as a query parameter
        String targetUrl = "http://localhost:5173/admin/login?token=" + token;

        // 3. Perform the redirect! This forces the browser to go back to your React app.
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}