package com.laserxprts.falcon.service;

import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.laserxprts.falcon.dto.response.NewsArticleResponse;
import com.laserxprts.falcon.model.NewsArticle;
import com.laserxprts.falcon.repository.NewsArticleRepository;
import com.laserxprts.falcon.repository.NewsCategoryRepository;

@Service
public class NewsArticleService {

    private final NewsArticleRepository newsArticleRepository;
    private final NewsCategoryRepository newsCategoryRepository;
    private final FileUploadService fileUploadService;

    public NewsArticleService(NewsArticleRepository newsArticleRepository, 
                            FileUploadService fileUploadService,
                            NewsCategoryRepository newsCategoryRepository) {
        this.newsArticleRepository = newsArticleRepository;
        this.fileUploadService = fileUploadService;
        this.newsCategoryRepository = newsCategoryRepository;
    }

    public Page<NewsArticleResponse> getAllNews(String category, String keyword, int page, int limit, String sortBy, String sortDirection, boolean isAdmin) {
        String sortField = (sortBy != null && !sortBy.isEmpty()) ? sortBy : "createdAt";
        Sort.Direction direction = Sort.Direction.DESC;
        if (sortDirection != null && sortDirection.equalsIgnoreCase("asc")) {
            direction = Sort.Direction.ASC;
        }
        
        Pageable pageable = PageRequest.of(
            Math.max(page - 1, 0), 
            limit, 
            Sort.by(direction, sortField)
        );

        // 1. Keyword AND Category
        if (keyword != null && !keyword.isBlank() && category != null && !category.isBlank()) {
            return isAdmin
                ? newsArticleRepository.searchByKeywordAndCategory(keyword, category, pageable).map(NewsArticleResponse::from)
                : newsArticleRepository.searchByKeywordAndCategoryAndPublishedTrue(keyword, category, pageable).map(NewsArticleResponse::from);
        }

        // 2. Only Keyword
        if (keyword != null && !keyword.isBlank()) {
            return isAdmin
                ? newsArticleRepository.searchByKeyword(keyword, pageable).map(NewsArticleResponse::from)
                : newsArticleRepository.searchByKeywordAndPublishedTrue(keyword, pageable).map(NewsArticleResponse::from);
        }

        // 3. Only Category
        if (category != null && !category.isEmpty()) {
            return isAdmin
                ? newsArticleRepository.findByCategoryIgnoreCase(category, pageable).map(NewsArticleResponse::from)
                : newsArticleRepository.findByCategoryIgnoreCaseAndPublishedTrue(category, pageable).map(NewsArticleResponse::from);
        }

        // 4. Fetch All
        return isAdmin
            ? newsArticleRepository.findAll(pageable).map(NewsArticleResponse::from)
            : newsArticleRepository.findByPublishedTrue(pageable).map(NewsArticleResponse::from);
    }

    public List<NewsArticleResponse> getLatestNews(int limit, boolean isAdmin) {
        Pageable pageable = PageRequest.of(
            0, 
            limit,
            Sort.by("createdAt").descending()
        );

        return isAdmin
            ? newsArticleRepository.findAll(pageable).map(NewsArticleResponse::from).getContent()
            : newsArticleRepository.findByPublishedTrue(pageable).map(NewsArticleResponse::from).getContent();
    }

    public NewsArticleResponse getBySlug(String slug, boolean isAdmin) {
        if (!isAdmin) {
            throw new RuntimeException("Unauthorised Access");
        }
        NewsArticleResponse news = NewsArticleResponse.from(newsArticleRepository.findBySlug(slug)
        .orElseThrow(() -> new RuntimeException("News Article Not Found")));
        return news;
    }

    public NewsArticleResponse createNewsArticle(NewsArticle news, List<MultipartFile> images) {
        if (news.getTitle() == null || news.getTitle().isBlank()) {
            throw new RuntimeException("Title is required");
        }

        // NEW: Validate Category against Database
        if (news.getCategory() == null || !newsCategoryRepository.existsByNameIgnoreCase(news.getCategory())) {
            throw new RuntimeException("Invalid or missing News Category. Please select a valid category from the database.");
        }

        String slug = generateSlug(news.getTitle());
        if (newsArticleRepository.existsBySlug(slug)) {
            throw new RuntimeException("News Already exists");
        }

        news.setSlug(slug);
        news.setCreatedAt(LocalDateTime.now());
        news.setUpdatedAt(LocalDateTime.now());

        if (images != null && !images.isEmpty()) {
            news.setImageUrls(fileUploadService.uploadMultipleImages(images));
        } else {
            news.setImageUrls(new ArrayList<>());
        }

        return NewsArticleResponse.from(newsArticleRepository.save(news));
    }

    public NewsArticleResponse updateNews(String id, NewsArticle updateNews, List<MultipartFile> images) {
        if (id == null || id.isBlank()) {
            throw new RuntimeException("Invalid ID");
        }

        NewsArticle existingNews = newsArticleRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("News not found"));

        if (updateNews.getTitle() != null && !updateNews.getTitle().isBlank()) {
            String newSlug = generateSlug(updateNews.getTitle());
            if (!newSlug.equals(existingNews.getSlug()) && newsArticleRepository.existsBySlug(newSlug)) {
                throw new RuntimeException("An article with this updated title/slug already exists");
            }
            existingNews.setTitle(updateNews.getTitle());
            existingNews.setSlug(newSlug);
        }

        if (updateNews.getContent() != null && !updateNews.getContent().isBlank()) {
            existingNews.setContent(updateNews.getContent());
        }

        if (updateNews.getCategory() != null && !updateNews.getCategory().isBlank()) {
            // NEW: Validate Category against Database
            if (!newsCategoryRepository.existsByNameIgnoreCase(updateNews.getCategory())) {
                throw new RuntimeException("Invalid News Category. Please select a valid category from the database.");
            }
            existingNews.setCategory(updateNews.getCategory());
        }

        if (images != null && !images.isEmpty()) {
            fileUploadService.deleteFiles(existingNews.getImageUrls());
            existingNews.setImageUrls(fileUploadService.uploadMultipleImages(images));
        }

        existingNews.setPublished(updateNews.isPublished());

        existingNews.setUpdatedAt(LocalDateTime.now());

        return NewsArticleResponse.from(newsArticleRepository.save(existingNews));
    }

    public void deleteNews(String id) {

        if (id == null) {
            throw new RuntimeException("Id can not be null");
        } 

        NewsArticle existingNews = newsArticleRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("News not found"));
        fileUploadService.deleteFiles(existingNews.getImageUrls());
        newsArticleRepository.deleteById(id);
    }

    public NewsArticleResponse togglePublishStatus(String id) {
        NewsArticle existingNews = newsArticleRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("News not found"));
        
        // Flip the current boolean value
        existingNews.setPublished(!existingNews.isPublished());
        existingNews.setUpdatedAt(LocalDateTime.now());
        
        return NewsArticleResponse.from(newsArticleRepository.save(existingNews));
    }

    private String generateSlug(String title) {
        return title.toLowerCase()
            .replaceAll("[^a-z0-9\\s]", "")
            .trim()
            .replaceAll("\\s+", "-");
    }

}
