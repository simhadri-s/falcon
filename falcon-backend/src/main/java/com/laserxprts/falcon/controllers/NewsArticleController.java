package com.laserxprts.falcon.controllers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import com.laserxprts.falcon.dto.request.NewsArticleRequest;
import com.laserxprts.falcon.dto.response.NewsArticleResponse;
import com.laserxprts.falcon.model.NewsArticle;
import com.laserxprts.falcon.security.PermissionService;
import com.laserxprts.falcon.service.NewsArticleService;

import jakarta.validation.Valid;

@PreAuthorize("@permissionService.hasAccess('MANAGE_NEWS')")
@RestController
@RequestMapping("/api/news")
public class NewsArticleController {

    private final NewsArticleService newsArticleService;
    private final PermissionService permissionService;

    public NewsArticleController(NewsArticleService newsArticleService, PermissionService permissionService) {
        this.newsArticleService = newsArticleService;
        this.permissionService = permissionService;
    }

    @PreAuthorize("permitAll()")
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllNews(
        @RequestParam(value = "category", required = false) String category,
        @RequestParam(value = "search", required = false) String keyword,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "10") int limit,
        @RequestParam(value = "sortBy", required = false) String sortBy,
        @RequestParam(value = "sortDirection", required = false) String sortDirection
    ) {
        Page<NewsArticleResponse> result = newsArticleService.getAllNews(category, keyword, page, limit, sortBy, sortDirection, isAdmin());

        Map<String, Object> response = new HashMap<>();
        response.put("data", result.getContent());
        response.put("total", result.getTotalElements());
        response.put("page", page);
        response.put("pages", result.getTotalPages());

        return ResponseEntity.ok(response);
    }

    @PreAuthorize("permitAll()")
    @GetMapping("/latest")
    public List<NewsArticleResponse> getLatestNews(
        @RequestParam(defaultValue = "3") int limit
    ) {
        return newsArticleService.getLatestNews(limit, isAdmin());
    }

    @PreAuthorize("permitAll()")
    @GetMapping("/{slug}")
    public NewsArticleResponse getBySlug(@PathVariable String slug) {
        return newsArticleService.getBySlug(slug, isAdmin());
    }

    @PostMapping
    public NewsArticleResponse createNewsArticle(@Valid @ModelAttribute NewsArticleRequest newsArticleRequest) {
        NewsArticle news = new NewsArticle();
        news.setTitle(newsArticleRequest.getTitle());
        news.setContent(newsArticleRequest.getContent());
        news.setCategory(newsArticleRequest.getCategory());

        return newsArticleService.createNewsArticle(news, newsArticleRequest.getImages());
    }

    @PutMapping("/{id}")
    public NewsArticleResponse updateNewsArticle(
        @PathVariable String id,
        @ModelAttribute NewsArticleRequest newsArticleRequest
    ) {
        NewsArticle updatedNews = new NewsArticle();
        updatedNews.setTitle(newsArticleRequest.getTitle());
        updatedNews.setContent(newsArticleRequest.getContent());
        updatedNews.setCategory(newsArticleRequest.getCategory());

        return newsArticleService.updateNews(id, updatedNews, newsArticleRequest.getImages());
    }

    @PatchMapping("/{id}/toggle-publish")
    public ResponseEntity<NewsArticleResponse> toggleNewsPublishStatus(@PathVariable String id) {
        return ResponseEntity.ok(newsArticleService.togglePublishStatus(id));
    }

    @DeleteMapping("/{id}")
    public Map<String, String> deleteNews(@PathVariable String id) {
        newsArticleService.deleteNews(id);
        return Map.of("message", "News deleted successfully");
    }

    private boolean isAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal().equals("anonymousUser")) {
            return false;
        }
        return permissionService.hasAccess("MANAGE_NEWS");
    }
}