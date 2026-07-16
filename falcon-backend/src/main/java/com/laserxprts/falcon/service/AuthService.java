package com.laserxprts.falcon.service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;

import com.laserxprts.falcon.dto.request.ResetRequest;
import com.laserxprts.falcon.exception.ApiException;
import com.laserxprts.falcon.exception.DuplicateResourceException;
import com.laserxprts.falcon.exception.ResourceNotFoundException;
import com.laserxprts.falcon.model.User;
import com.laserxprts.falcon.model.Otp;
import com.laserxprts.falcon.model.Role;
import com.laserxprts.falcon.repository.UserRepository;
import com.laserxprts.falcon.repository.OtpRepository;
import com.laserxprts.falcon.repository.RoleRepository;
import com.laserxprts.falcon.utils.JwtUtils;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final OtpRepository otpRepository;
    private final RoleRepository roleRepository; // Added RoleRepository
    private final EmailService emailService;
    private final JwtUtils jwtUtils;
    private final BCryptPasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, 
                       OtpRepository otpRepository, 
                       RoleRepository roleRepository, // Injected here
                       JwtUtils jwtUtils, 
                       EmailService emailService,
                       BCryptPasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.otpRepository = otpRepository;
        this.roleRepository = roleRepository;
        this.jwtUtils = jwtUtils;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
    }

    public String login(String email, String password) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));
            
        if(passwordEncoder.matches(password, user.getPasswordHash())) {
            // Ensure your JwtUtils handles the Set<Role> correctly
            return jwtUtils.generateToken(user.getEmail(), user.getRoles(), user.getTokenVersion());
        } else {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }
    }   

    public User register(String name, String email, String password) {
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("User already exists");
        }

        validatePasswordStrength(password);
        String hashedPassword = passwordEncoder.encode(password);
        User user = new User();
        user.setEmail(email);
        user.setName(name);
        user.setPasswordHash(hashedPassword);

        Set<Role> roles = new HashSet<>();

        if (userRepository.count() == 0) {
            // Fetch the role from DB, or create it if it doesn't exist
            Role superAdminRole = roleRepository.findByName("SUPER_ADMIN")
                .orElseGet(() -> {
                    Role role = new Role();
                    role.setName("SUPER_ADMIN");
                    role.setPermissions(List.of("ALL_PERMISSIONS"));
                    return roleRepository.save(role);
                });
            roles.add(superAdminRole);
        } else {
            // Standard user role
            Role userRole = roleRepository.findByName("USER")
                .orElseGet(() -> {
                    Role role = new Role();
                    role.setName("USER");
                    role.setPermissions(List.of("BASIC_ACCESS"));
                    return roleRepository.save(role);
                });
            roles.add(userRole);
        }

        user.setRoles(roles);
        return userRepository.save(user);
    }

    public User updateDetails(String name, String email, String password, String currentEmail) {
        User updatedUser = userRepository.findByEmail(currentEmail)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (name != null && !name.isBlank()) {
            updatedUser.setName(name);
        }
        if (email != null && !email.isBlank()) {
            updatedUser.setEmail(email);
        }
        if (password != null && !password.isBlank()) {
            validatePasswordStrength(password);
            String hashedPassword = passwordEncoder.encode(password);
            updatedUser.setPasswordHash(hashedPassword);
        }
        return userRepository.save(updatedUser);
    }

    public boolean sendOtp(String email) {
        if (userRepository.existsByEmail(email)) {
            java.security.SecureRandom random = new java.security.SecureRandom();
            int code = 100000 + random.nextInt(900000);
            
            // 1. Generate OTP via EmailService
            emailService.sendOtpHtmlAsync(email, code);
            
            // 2. Clear any previous OTP for this email
            otpRepository.deleteByEmail(email);

            // 3. Save new OTP to MongoDB
            Otp otpEntry = new Otp();
            otpEntry.setEmail(email);
            otpEntry.setOtpCode(Integer.toString(code));
            otpEntry.setExpiryTime(LocalDateTime.now().plusMinutes(5));
            otpRepository.save(otpEntry);
            
            return true;
        }
        return false;
    }

    public String resetPassword(ResetRequest resetRequest) {
        // 1. Fetch from DB
        Otp storedOtp = otpRepository.findByEmail(resetRequest.getEmail())
                .orElse(null);

        // 2. Use .equals() for String comparison
        if (storedOtp != null && 
            storedOtp.getOtpCode().equals(resetRequest.getOtp()) && 
            LocalDateTime.now().isBefore(storedOtp.getExpiryTime())) {

            User user = userRepository.findByEmail(resetRequest.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
            
            validatePasswordStrength(resetRequest.getPassword());
            user.setPasswordHash(passwordEncoder.encode(resetRequest.getPassword()));
            userRepository.save(user);

            otpRepository.delete(storedOtp);

            return "Successfully changed the password";
        }
        
        return "Invalid OTP or OTP has expired";
    }

    private void validatePasswordStrength(String password) {
        if (password == null || password.length() < 8) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Password must be at least 8 characters long");
        }
        
        boolean hasUppercase = password.chars().anyMatch(Character::isUpperCase);
        boolean hasLowercase = password.chars().anyMatch(Character::isLowerCase);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);
        boolean hasSpecial = password.chars().anyMatch(ch -> "!@#$%^&*()-_=+[{]};:'\",<.>/?\\|`~".indexOf(ch) >= 0);
        
        if (!hasUppercase || !hasLowercase || !hasDigit || !hasSpecial) {
            throw new ApiException(HttpStatus.BAD_REQUEST, 
                "Password must be at least 8 characters long, contain at least one uppercase letter, one lowercase letter, one digit, and one special character");
        }
    }
}
