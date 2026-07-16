package com.laserxprts.falcon.dto.request;

import java.util.Map;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NotificationRequest {
    @NotBlank(message = "Title is required")
    private String title;
    
    @NotBlank(message = "Body is required")
    private String body;
    
    private String imageUrl;
    
    private MultipartFile image;
    
    private Map<String, String> data;
}
