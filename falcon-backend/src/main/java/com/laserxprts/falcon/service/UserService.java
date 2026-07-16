package com.laserxprts.falcon.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.laserxprts.falcon.dto.response.UserResponse;
import com.laserxprts.falcon.model.Role;
import com.laserxprts.falcon.model.User;
import com.laserxprts.falcon.repository.RoleRepository;
import com.laserxprts.falcon.repository.UserRepository;
import com.laserxprts.falcon.repository.AddressRepository;
import com.laserxprts.falcon.repository.CartRepository;
import com.laserxprts.falcon.repository.WishlistRepository;
import com.laserxprts.falcon.repository.ReviewRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {
    
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final AddressRepository addressRepository;
    private final CartRepository cartRepository;
    private final WishlistRepository wishlistRepository;
    private final ReviewRepository reviewRepository;

    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
            .map(UserResponse::from)
            .toList();
    }

    public List<UserResponse> getByRole(String roleName, String keyword) {
        Role role = roleRepository.findByName(roleName)
            .orElseThrow(() -> new RuntimeException("Role Name not found"));
            
        if (keyword != null && !keyword.isBlank()) {
            return userRepository.findByRolesAndKeyword(role, keyword).stream()
                .map(UserResponse::from)
                .toList();
        }

        return userRepository.findByRoles(role).stream()
            .map(UserResponse::from)
            .toList();
    }

   
    public UserResponse getUserById(String id) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("User not found with ID: " + id));
        return UserResponse.from(user);
    }

    
    public UserResponse getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("User not found with email: " + email));
        return UserResponse.from(user);
    }

    /**
     * Completely remove a user from the database.
     */
    public void deleteUser(String id) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("User not found with ID: " + id));

        // Security check: Prevent deleting a Super Admin to avoid locking yourself out
        boolean isSuperAdmin = user.getRoles().stream()
                .anyMatch(role -> role.getName().equals("SUPER_ADMIN"));
                
        if (isSuperAdmin) {
            throw new RuntimeException("Cannot delete a SUPER_ADMIN account.");
        }

        addressRepository.deleteByUserId(id);
        cartRepository.deleteByUserId(id);
        wishlistRepository.deleteByUserId(id);
        reviewRepository.deleteByUserId(id);

        userRepository.deleteById(id);
    }

    public void addFcmToken(String email, String token) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("User not found"));
        if (user.getFcmTokens() == null) {
            user.setFcmTokens(new java.util.HashSet<>());
        }
        user.getFcmTokens().add(token);
        userRepository.save(user);
    }

    public void removeFcmToken(String email, String token) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("User not found"));
        if (user.getFcmTokens() != null) {
            user.getFcmTokens().remove(token);
            userRepository.save(user);
        }
    }

}