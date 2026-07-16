package com.laserxprts.falcon.service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.laserxprts.falcon.dto.response.UserResponse;
import com.laserxprts.falcon.exception.ApiException;
import com.laserxprts.falcon.exception.DuplicateResourceException;
import com.laserxprts.falcon.exception.ResourceNotFoundException;
import com.laserxprts.falcon.model.Role;
import com.laserxprts.falcon.model.User;
import com.laserxprts.falcon.repository.RoleRepository;
import com.laserxprts.falcon.repository.UserRepository;

@Service
public class RoleService {
    
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;

    public RoleService(RoleRepository roleRepository, UserRepository userRepository) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
    }

    public UserResponse allocateRole(String email, List<String> roleNames) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
            
        Set<Role> roles = roleNames.stream()
            .map(roleName -> roleRepository.findByName(roleName)
                    .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + roleName))
            ).collect(Collectors.toSet());
            
        user.setRoles(roles);
        
        userRepository.save(user); 
        
        return UserResponse.from(user);
    }

    public List<Role> getAllRoles() {
        return roleRepository.findAll().stream()
            .filter(role -> !"SUPER_ADMIN".equals(role.getName()))
            .collect(Collectors.toList());
    } 

    public Role createRole(Role role) {
        if (role.getName() == null || role.getName().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Role name cannot be null or empty");
        }
        
        String upperCaseName = role.getName().toUpperCase();
        
        // NEW: Check if it already exists
        if (roleRepository.findByName(upperCaseName).isPresent()) {
            throw new DuplicateResourceException("Role '" + upperCaseName + "' already exists.");
        }
        
        Role newRole = new Role();
        newRole.setName(upperCaseName);
        
        if (role.getPermissions() != null) {
            newRole.setPermissions(role.getPermissions().stream()
                .filter(s -> s != null && !s.isBlank()) 
                .map(String::toUpperCase)
                .collect(Collectors.toList())
            );
        } else {
            newRole.setPermissions(List.of()); 
        }
        
        return roleRepository.save(newRole);
    }

    public Role updateRolePermissions(String roleName, List<String> newPermissions) {
        String upperCaseName = roleName.toUpperCase();
        
        Role existingRole = roleRepository.findByName(upperCaseName)
            .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + upperCaseName));

        if ("SUPER_ADMIN".equals(upperCaseName)) {
            throw new ApiException(HttpStatus.CONFLICT, "Cannot modify SUPER_ADMIN permissions directly.");
        }

        if (newPermissions != null) {
            existingRole.setPermissions(newPermissions.stream()
                .filter(s -> s != null && !s.isBlank())
                .map(String::toUpperCase)
                .collect(Collectors.toList())
            );
        } else {
            existingRole.setPermissions(List.of());
        }

        return roleRepository.save(existingRole);
    }

    // 3. NEW METHOD: Delete a role safely
    public void deleteRole(String roleName) {
        String upperCaseName = roleName.toUpperCase();
        
        Role existingRole = roleRepository.findByName(upperCaseName)
            .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + upperCaseName));

        // Security check: Protect base system roles
        if ("SUPER_ADMIN".equals(upperCaseName) || "USER".equals(upperCaseName)) {
            throw new ApiException(HttpStatus.CONFLICT, "Cannot delete critical system roles.");
        }

        roleRepository.delete(existingRole);
        
        // Remove the role from any users who currently have it to prevent broken DBRefs
        List<User> usersWithRole = userRepository.findByRoles(existingRole);
        if (!usersWithRole.isEmpty()) {
            for (User user : usersWithRole) {
                user.getRoles().remove(existingRole);
            }
            userRepository.saveAll(usersWithRole);
        }
    }
}