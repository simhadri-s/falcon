package com.laserxprts.falcon.model;

import lombok.Getter;
import lombok.Setter;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDateTime;

@Getter
@Setter
@Document(collection = "otps")
public class Otp {
    @Id
    private String id;

    @Indexed(unique = true)
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank
    @Pattern(regexp = "^[0-9]{6}$", message = "OTP must be exaclty 6 digits")
    private String otpCode;

    // Pro Tip: MongoDB can auto-delete this after 5 minutes (300 seconds)
    @Indexed(expireAfterSeconds = 300) 
    private LocalDateTime expiryTime;
}