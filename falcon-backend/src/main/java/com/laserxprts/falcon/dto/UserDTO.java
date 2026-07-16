package com.laserxprts.falcon.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class UserDTO {
    @NotNull(message = "Username cannot be null")
    @Size(min = 3, max = 20)
    private String name;

    @NotEmpty(message = "Email cannot be empty")
    @Email(message = "Invalid email format", regexp = ".+@.+\\..+")
    private String email;
}