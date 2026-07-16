package com.laserxprts.falcon.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.laserxprts.falcon.exception.ApiException;
import com.laserxprts.falcon.model.Role;
import com.laserxprts.falcon.repository.RoleRepository;
import com.laserxprts.falcon.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
public class RoleServiceTest {

    @Mock private RoleRepository roleRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private RoleService roleService;

    @Test
    void testCreateRole_Success() {
        Role req = new Role();
        req.setName("MANAGER");
        
        when(roleRepository.findByName("MANAGER")).thenReturn(Optional.empty());
        when(roleRepository.save(any(Role.class))).thenReturn(req);

        Role result = roleService.createRole(req);

        assertNotNull(result);
        assertEquals("MANAGER", result.getName());
    }

    @Test
    void testCreateRole_NullName_ThrowsException() {
        Role req = new Role();
        
        ApiException ex = assertThrows(ApiException.class, () -> roleService.createRole(req));
        assertEquals("Role name cannot be null or empty", ex.getMessage());
    }
}
