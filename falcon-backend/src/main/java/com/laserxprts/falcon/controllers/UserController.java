package com.laserxprts.falcon.controllers;

import java.security.Principal;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.laserxprts.falcon.dto.response.UserResponse;
import com.laserxprts.falcon.service.UserService;

@RestController
@RequestMapping("/api/users")
@PreAuthorize("@permissionService.hasAccess('ALL_PERMISSIONS')")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @GetMapping("/role")
    public ResponseEntity<List<UserResponse>> getByRole(
        @RequestParam("name") String roleName,
        @RequestParam(value = "search", required = false) String keyword
    ) {
        return ResponseEntity.ok(userService.getByRole(roleName, keyword));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable String id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteUser(@PathVariable String id) {
        userService.deleteUser(id);
        return ResponseEntity.ok(Map.of("message", "User deleted successfully"));
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()") 
    public ResponseEntity<UserResponse> getMyProfile(Principal principal) {
        String userEmail = principal.getName();
        return ResponseEntity.ok(userService.getUserByEmail(userEmail));
    }

    @PostMapping("/fcm-token")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, String>> addFcmToken(Principal principal, @RequestBody com.laserxprts.falcon.dto.request.FCMTokenRequest request) {
        userService.addFcmToken(principal.getName(), request.getToken());
        return ResponseEntity.ok(Map.of("message", "Token registered"));
    }

    @DeleteMapping("/fcm-token")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, String>> removeFcmToken(Principal principal, @RequestBody com.laserxprts.falcon.dto.request.FCMTokenRequest request) {
        userService.removeFcmToken(principal.getName(), request.getToken());
        return ResponseEntity.ok(Map.of("message", "Token removed"));
    }
}