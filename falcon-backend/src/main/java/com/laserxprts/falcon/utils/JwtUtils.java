package com.laserxprts.falcon.utils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.laserxprts.falcon.model.Role;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;

@Component
public class JwtUtils {
    
    @Value("${app.jwtSecret}")
    private String jwtSecret;

    @Value("${app.jwtExpirationMs}")
    private long jwtExpirationMs;

    private SecretKey key;

    @PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    public String generateToken(String email, Set<Role> roles, long tokenVersion) {
        // 1. Flatten the Set<Role> into a simple List<String> of authorities
        List<String> authorities = new ArrayList<>();
        if (roles != null) {
            for (Role role : roles) {
                // Add the base role (e.g., "ROLE_SUPER_ADMIN")
                authorities.add("ROLE_" + role.getName());
                
                // Add all granular permissions (e.g., "CREATE_JOB", "PUBLISH_NEWS")
                if (role.getPermissions() != null) {
                    authorities.addAll(role.getPermissions());
                }
            }
        }

        return Jwts.builder()
            .claims()
                .subject(email)
                .add("authorities", authorities) // Store the flattened list in the token
                .add("tokenVersion", tokenVersion)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
            .and()
            .signWith(key)
            .compact();
    }

    /**
     * Parses the token ONCE and returns the Claims.
     * Returns null if the token is invalid or expired.
     */
    public Claims parseClaims(String token) {
        try {
            return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }

    public String extractEmail(Claims claims) {
        return claims.getSubject();
    }

    @SuppressWarnings("unchecked")
    public List<String> extractAuthorities(Claims claims) {
        return claims.get("authorities", List.class);
    }

    public Long extractTokenVersion(Claims claims) {
        return claims.get("tokenVersion", Long.class);
    }

    // Keep original single-arg methods for any other callers
    public String extractEmail(String token) {
        return Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .getPayload()
            .getSubject();
    }

    // 2. NEW: Extracts the list of authorities from the token for JwtAuthFilter
    @SuppressWarnings("unchecked")
    public List<String> extractAuthorities(String token) {
        return Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .getPayload()
            .get("authorities", List.class);
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            System.err.println("Invalid JWT token: " + e.getMessage());
            return false;
        }
    }
}