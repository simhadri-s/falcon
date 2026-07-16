package com.laserxprts.falcon.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component("permissionService")
public class PermissionService {
    
    public boolean hasAccess(String requiredPermission) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        return authentication.getAuthorities().stream()
            .anyMatch(auth ->
                auth.getAuthority().equals(requiredPermission) ||
                auth.getAuthority().equals("ALL_PERMISSIONS")
            );
    }
}
