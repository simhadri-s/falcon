package com.laserxprts.falcon.model;

import java.time.LocalDateTime;

import org.hibernate.validator.constraints.URL;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Document(collection = "job_applications")
public class JobApplication{

    @Id
    private String id;

    @NotBlank(message = "Name cannot be empty")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters" )
    private String name;

    @NotBlank
    @Email(message = "Email is invalid")
    private String  email;
     
    @Pattern(regexp = "^[0-9]{10}$", message = "Phone number must be 10 digits")
    private String phone;

    @URL(message = "Must be a valid URL format")
    private String cvUrl;

    @URL(message = "Must be a valid URL format")
    private String coverLetterUrl;

    @DBRef
    private Job job;

    @CreatedDate
    private LocalDateTime createdAt;

}
