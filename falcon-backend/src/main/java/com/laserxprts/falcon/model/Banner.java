package com.laserxprts.falcon.model;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.mongodb.lang.NonNull;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Document(collection = "banners" )
public class Banner {
    @Id
    private String id;

    @NonNull
    private String imageUrl;

    private String title;
    private String description;

    private boolean active;
    @Indexed(unique = true, partialFilter = "{ 'isActive': true }")
    private boolean defaultBanner;

    @CreatedDate
    private LocalDateTime createdAt;
    
}
