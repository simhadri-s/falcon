package com.laserxprts.falcon.model;

import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
@Document(collection = "notifications")
public class Notification {
    
    @Id
    private String id;
    
    // Null if it's a broadcast to everyone
    private String userId;
    
    private String title;
    private String body;
    private String imageUrl;
    
    // Optional data like orderId or type
    private Map<String, String> data;
    
    private boolean read;
    
    @CreatedDate
    private LocalDateTime createdAt;
}
