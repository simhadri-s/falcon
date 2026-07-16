package com.laserxprts.falcon.model;

import java.util.HashSet;
import java.util.Set;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Document(collection = "users")
public class User {
    
    @Id
    private String id;

    @Version
    private Long version;
    
    private long tokenVersion = 0L;
    
    @NotBlank
    private String name;
    @NotBlank
    @Email(message = "Invalid email format", regexp = ".+@.+\\..+")
    @Indexed(unique = true)
    private String email;

    @NotBlank
    private String passwordHash;

    @DBRef
    private Set<Role> roles = new HashSet<>();

    private Set<String> fcmTokens = new HashSet<>();

}
