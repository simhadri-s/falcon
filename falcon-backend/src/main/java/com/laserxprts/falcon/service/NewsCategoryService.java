package com.laserxprts.falcon.service;

import java.util.List;
import org.springframework.stereotype.Service;

import com.laserxprts.falcon.model.NewsCategory;
import com.laserxprts.falcon.repository.NewsCategoryRepository;
import com.laserxprts.falcon.model.NewsArticle;
import com.laserxprts.falcon.repository.NewsArticleRepository;

@Service
public class NewsCategoryService {
    
    private final NewsCategoryRepository categoryRepository;
    private final NewsArticleRepository articleRepository;

    public NewsCategoryService(NewsCategoryRepository categoryRepository, NewsArticleRepository articleRepository) {
        this.categoryRepository = categoryRepository;
        this.articleRepository = articleRepository;
    }

    public List<NewsCategory> getAllCategories() {
        return categoryRepository.findAll();
    }

    public NewsCategory createCategory(NewsCategory category) {
        if (category.getName() == null || category.getName().isBlank()) {
            throw new RuntimeException("Category name is required");
        }
        
        if (categoryRepository.existsByNameIgnoreCase(category.getName())) {
            throw new RuntimeException("News category already exists");
        }

        category.setSlug(generateSlug(category.getName()));
        return categoryRepository.save(category);
    }

    public void deleteCategory(String id) {
        NewsCategory category = categoryRepository.findById(id).orElse(null);
        if (category != null) {
            List<NewsArticle> articles = articleRepository.findByCategoryIgnoreCase(category.getName());
            if (!articles.isEmpty()) {
                for (NewsArticle article : articles) {
                    article.setCategory(null);
                }
                articleRepository.saveAll(articles);
            }
        }

        categoryRepository.deleteById(id);
    }

    private String generateSlug(String title) {
        return title.toLowerCase().replaceAll("[^a-z0-9\\s]", "").trim().replaceAll("\\s+", "-");
    }
}