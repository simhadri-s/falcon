package com.laserxprts.falcon.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.laserxprts.falcon.exception.ApiException;
import com.laserxprts.falcon.model.Role;
import com.laserxprts.falcon.model.User;
import com.laserxprts.falcon.repository.OtpRepository;
import com.laserxprts.falcon.repository.RoleRepository;
import com.laserxprts.falcon.repository.UserRepository;
import com.laserxprts.falcon.utils.JwtUtils;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private OtpRepository otpRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private EmailService emailService;
    @Mock private JwtUtils jwtUtils;
    @Mock private BCryptPasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    private User testUser;
    private Role customerRole;

    @BeforeEach
    void setUp() {
        customerRole = new Role();
        customerRole.setId("1");
        customerRole.setName("CUSTOMER");
        
        testUser = new User();
        testUser.setId("100");
        testUser.setEmail("test@example.com");
        testUser.setPasswordHash("hashed_password");
        testUser.setRoles(Set.of(customerRole));
        testUser.setTokenVersion(1L);
    }

    @Test
    void testLogin_Success() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password123", "hashed_password")).thenReturn(true);
        when(jwtUtils.generateToken(eq("test@example.com"), any(), eq(1L))).thenReturn("mocked_jwt_token");

        String token = authService.login("test@example.com", "password123");

        assertEquals("mocked_jwt_token", token);
        verify(userRepository, times(1)).findByEmail("test@example.com");
    }

    @Test
    void testLogin_InvalidPassword_ThrowsException() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrongpass", "hashed_password")).thenReturn(false);

        ApiException ex = assertThrows(ApiException.class, () -> authService.login("test@example.com", "wrongpass"));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
    }

    @Test
    void testRegister_Success() {
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(userRepository.count()).thenReturn(1L);
        when(roleRepository.findByName("USER")).thenReturn(Optional.of(customerRole));
        when(passwordEncoder.encode("StrongP@ssw0rd!")).thenReturn("hashed_new");
        
        User savedUser = new User();
        savedUser.setEmail("new@example.com");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        User result = authService.register("New User", "new@example.com", "StrongP@ssw0rd!");

        assertNotNull(result);
        assertEquals("new@example.com", result.getEmail());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void testRegister_DuplicateEmail_ThrowsException() {
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        ApiException ex = assertThrows(ApiException.class, () -> authService.register("Test", "test@example.com", "pass"));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
    }
}
