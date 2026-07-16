package com.laserxprts.falcon.controllers;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.laserxprts.falcon.exception.ApiException;
import com.laserxprts.falcon.dto.response.UserResponse;
import com.laserxprts.falcon.model.Role;
import com.laserxprts.falcon.service.RoleService;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api/roles")
@PreAuthorize("@permissionService.hasAccess('ALL_PERMISSIONS')")
public class RoleController {
    
    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    // 1. GET: Fetch all roles (Except SUPER_ADMIN)
    @GetMapping
    public ResponseEntity<List<Role>> getAllRoles() {
        return ResponseEntity.ok(roleService.getAllRoles());
    }

    // 2. POST: Create a new role
    @PostMapping
    public ResponseEntity<Role> createRole(@Valid @RequestBody Role role) {
        return ResponseEntity.ok(roleService.createRole(role));
    }


    @PutMapping("/{roleName}/permissions")
    public ResponseEntity<Role> updateRolePermissions(
            @PathVariable String roleName, 
            @RequestBody List<String> newPermissions) {
        return ResponseEntity.ok(roleService.updateRolePermissions(roleName, newPermissions));
    }

    // 4. DELETE: Remove a role
    // URL Example: DELETE /api/roles/GUEST
    @DeleteMapping("/{roleName}")
    public ResponseEntity<Map<String, String>> deleteRole(@PathVariable String roleName) {
        roleService.deleteRole(roleName);
        return ResponseEntity.ok(Map.of("message", "Role '" + roleName + "' deleted successfully"));
    }

    // 5. PUT: Allocate roles to a specific user
    // Now it takes the target user's email from the request body, NOT the principal
    @PutMapping("/allocate")
    public ResponseEntity<UserResponse> allocateRole(@RequestBody Map<String, Object> request) {
        // Extract data safely from the Map
        String targetEmail = (String) request.get("email");
        
        @SuppressWarnings("unchecked")
        List<String> roleNames = (List<String>) request.get("roleNames");

        if (targetEmail == null || roleNames == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Both email and roleNames are required");
        }

        return ResponseEntity.ok(roleService.allocateRole(targetEmail, roleNames));
    }
}
