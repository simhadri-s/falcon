package com.laserxprts.falcon.model;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Document( collection = "news_articles")
public class NewsArticle {
    
    @Id
    private String id;

    @NotBlank
    @Size(max = 200, message = "Title cannot exceed 200 characters")
    private String title;

    @Pattern(regexp = "^[a-z0-9-]+$", message = "Slug must be URL-safe (lowercase, numbers, hyphens only")
    @Indexed(unique = true)
    private String slug;

    private String content;
    @Indexed
    private String category;
    private List<String> imageUrls;
    private String author;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Indexed
    private boolean published = false;
}
