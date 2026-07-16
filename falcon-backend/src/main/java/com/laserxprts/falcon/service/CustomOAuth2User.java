package com.laserxprts.falcon.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import com.laserxprts.falcon.model.Role;
import com.laserxprts.falcon.model.User;

public class CustomOAuth2User implements OAuth2User {
    private final User user;
    private final Map<String, Object> attributes;

    public CustomOAuth2User(User user, Map<String, Object> attributes) {
        this.user = user;
        this.attributes = attributes;
    }

    @Override
    public Map<String, Object> getAttributes() { 
        return attributes;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        List<GrantedAuthority> authorities = new ArrayList<>();
        
        // Loop through all roles assigned to this user
        if (user.getRoles() != null) {
            for (Role role : user.getRoles()) {
                // Optional: Add the role name itself (e.g., "ROLE_USER")
                authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getName()));
                
                // CRITICAL: Extract all granular permissions (e.g., "PUBLISH_NEWS")
                if (role.getPermissions() != null) {
                    for (String permission : role.getPermissions()) {
                        authorities.add(new SimpleGrantedAuthority(permission));
                    }
                }
            }
        }
        return authorities;
    }

    @Override
    public String getName() { 
        return user.getEmail();
    }

    // Renamed this from getAdmin() to getUser() to match the return type
    public User getUser() { 
        return user; 
    }
}