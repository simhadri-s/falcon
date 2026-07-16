package com.laserxprts.falcon.dto.request;

import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class JobApplicationRequest {
    
    @NotBlank
    private String name;

    @NotBlank
    private String email;

    private MultipartFile cv;
    private MultipartFile coverLetter;

    @NotBlank
    @Pattern(regexp = "^[0-9]{10}$", message = "Phone number must be 10 digits")
    private String phone;

    @NotBlank
    private String jobId;
}
