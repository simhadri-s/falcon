package com.laserxprts.falcon.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import com.laserxprts.falcon.model.NewsArticle;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class NewsArticleResponse {
    private String id;
    private String title;
    private String slug;
    private String content;
    private String category;
    private String author;
    private LocalDateTime createdAt;
    private List<String> imageUrls;
    private boolean published;

    public static NewsArticleResponse from(NewsArticle article) {
        if (article == null) {
            return null;
        }
        
        return NewsArticleResponse.builder()
                .id(article.getId())
                .title(article.getTitle())
                .slug(article.getSlug())
                .content(article.getContent())
                .category(article.getCategory())
                .author(article.getAuthor())
                .createdAt(article.getCreatedAt())
                .imageUrls(article.getImageUrls())
                .published(article.isPublished())
                .build();
    }
}