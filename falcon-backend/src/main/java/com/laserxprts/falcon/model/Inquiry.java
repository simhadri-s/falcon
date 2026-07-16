package com.laserxprts.falcon.model;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Getter
@Setter
@Document(collection = "inquiries")
public class Inquiry{
@Id
private String id;

@NotBlank(message = "Name can not be empty")
private String name;

@Email(message = "Invalid email format", regexp = ".+@.+\\..+")
@NotBlank(message = "Email can not be null")
private String email;

@NotBlank(message = "Subject can not be null")
private String subject;

@Size(max = 2000, message = "Message cannot exceed 2000 characters")
private String message;

private String status = "NEW";


@CreatedDate
private LocalDateTime createdAt;


@NotBlank(message = "Phone number is required")
@Pattern(
    regexp = "^[0-9]{10}$",
    message = "Phone number must be exactly 10 digits and no special characters"
)
private String phone;
}


