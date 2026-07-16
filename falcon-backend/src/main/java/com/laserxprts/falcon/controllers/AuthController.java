package com.laserxprts.falcon.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.laserxprts.falcon.dto.request.ResetRequest;
import com.laserxprts.falcon.dto.request.UserRequest;
import com.laserxprts.falcon.exception.ApiException;
import com.laserxprts.falcon.exception.ResourceNotFoundException;
import com.laserxprts.falcon.model.User;
import com.laserxprts.falcon.service.AuthService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;

import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController { 

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody UserRequest request) {
        User user = authService.register(request.getName(), request.getEmail(), request.getPassword());
        return ResponseEntity.ok(Map.of("message", "User registered successfully", "email", user.getEmail()));
    }

    @PutMapping("/update")
    public ResponseEntity<?> updateDetails(@RequestBody UserRequest request, Principal principal) {
        User user = authService.updateDetails(request.getName(), request.getEmail(), request.getPassword(), principal.getName());
        return ResponseEntity.ok(Map.of("message", "User Details Updated successfully", "email", user.getEmail()));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> request) {
        String token = authService.login(request.get("email"), request.get("password"));
        return ResponseEntity.ok(Map.of("token", token));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> request) {
        boolean isSent = authService.sendOtp(request.get("email"));
        if (isSent) {
            return ResponseEntity.ok(Map.of("message", "OTP sent to your email"));
        }
        throw new ResourceNotFoundException("Email not found");
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetRequest resetRequest) {
        String result = authService.resetPassword(resetRequest);
        if (result.equals("Successfully changed the password")) {
            return ResponseEntity.ok(Map.of("message", result));
        }
        throw new ApiException(HttpStatus.BAD_REQUEST, result);
    }
}
