package com.laserxprts.falcon.security;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.lang.NonNull; 

import com.laserxprts.falcon.utils.JwtUtils;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;
    private final com.laserxprts.falcon.repository.UserRepository userRepository;

    public JwtAuthFilter(JwtUtils jwtUtils, com.laserxprts.falcon.repository.UserRepository userRepository) {
        this.jwtUtils = jwtUtils;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(
        @NonNull HttpServletRequest request, 
        @NonNull HttpServletResponse response, 
        @NonNull FilterChain filterChain) 
        throws ServletException, IOException {

            String authHeader = request.getHeader("Authorization");

            if (authHeader != null && authHeader.startsWith("Bearer ")) {

                String token = authHeader.substring(7);

                // Parse the token ONCE — validates signature + extracts all claims
                Claims claims = jwtUtils.parseClaims(token);

                if (claims != null) {
                    String email = jwtUtils.extractEmail(claims);
                    
                    if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                        
                        // NEW: Database check!
                        com.laserxprts.falcon.model.User user = userRepository.findByEmail(email).orElse(null);
                        if (user == null) {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json");
                            response.getWriter().write("{\"error\": \"User no longer exists\"}");
                            return;
                        }

                        Long tokenVersion = jwtUtils.extractTokenVersion(claims);
                        if (tokenVersion == null || tokenVersion < user.getTokenVersion()) {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json");
                            response.getWriter().write("{\"error\": \"Token has been invalidated. Please log in again.\"}");
                            return;
                        }
                        
                        // 1. Extract a LIST of strings representing roles AND permissions
                        List<String> authorityStrings = jwtUtils.extractAuthorities(claims); 
                        
                        if (authorityStrings == null || authorityStrings.isEmpty()) {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json");
                            response.getWriter().write("{\"error\": \"Token has no roles or permissions\"}");
                            return;
                        }

                        // 2. Map the strings into Spring Security's SimpleGrantedAuthority objects
                        List<SimpleGrantedAuthority> authorities = authorityStrings.stream()
                                .map(SimpleGrantedAuthority::new)
                                .collect(Collectors.toList());

                        // 3. Pass the full list of authorities to the authentication token
                        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            email, 
                            null, 
                            authorities
                        );

                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    }
                }
                else {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"error\": \"Token invalid or expired\"}");
                    return;
                }
            }
            
            filterChain.doFilter(request, response);
    }
}