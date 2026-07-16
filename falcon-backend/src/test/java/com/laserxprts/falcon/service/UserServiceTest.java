package com.laserxprts.falcon.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.laserxprts.falcon.dto.response.UserResponse;
import com.laserxprts.falcon.model.Role;
import com.laserxprts.falcon.model.User;
import com.laserxprts.falcon.repository.AddressRepository;
import com.laserxprts.falcon.repository.CartRepository;
import com.laserxprts.falcon.repository.ReviewRepository;
import com.laserxprts.falcon.repository.RoleRepository;
import com.laserxprts.falcon.repository.UserRepository;
import com.laserxprts.falcon.repository.WishlistRepository;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private AddressRepository addressRepository;
    @Mock private CartRepository cartRepository;
    @Mock private WishlistRepository wishlistRepository;
    @Mock private ReviewRepository reviewRepository;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private Role testRole;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId("USER-1");
        testUser.setEmail("test@test.com");
        testUser.setName("Test");

        testRole = new Role();
        testRole.setName("CUSTOMER");
    }

    @Test
    void testGetAllUsers() {
        when(userRepository.findAll()).thenReturn(List.of(testUser));

        List<UserResponse> result = userService.getAllUsers();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("test@test.com", result.get(0).getEmail());
    }

    @Test
    void testGetByRole_WithoutKeyword() {
        when(roleRepository.findByName("CUSTOMER")).thenReturn(Optional.of(testRole));
        when(userRepository.findByRoles(testRole)).thenReturn(List.of(testUser));

        List<UserResponse> result = userService.getByRole("CUSTOMER", null);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void testGetUserById_Success() {
        when(userRepository.findById("USER-1")).thenReturn(Optional.of(testUser));

        UserResponse result = userService.getUserById("USER-1");

        assertNotNull(result);
        assertEquals("test@test.com", result.getEmail());
    }

    @Test
    void testDeleteUser_Success() {
        when(userRepository.findById("USER-1")).thenReturn(Optional.of(testUser));

        userService.deleteUser("USER-1");

        verify(addressRepository).deleteByUserId("USER-1");
        verify(cartRepository).deleteByUserId("USER-1");
        verify(wishlistRepository).deleteByUserId("USER-1");
        verify(reviewRepository).deleteByUserId("USER-1");
        verify(userRepository).deleteById("USER-1");
    }
}
