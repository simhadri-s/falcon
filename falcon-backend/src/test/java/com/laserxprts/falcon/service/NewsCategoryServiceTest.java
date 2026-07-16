package com.laserxprts.falcon.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.laserxprts.falcon.model.NewsArticle;
import com.laserxprts.falcon.model.NewsCategory;
import com.laserxprts.falcon.repository.NewsArticleRepository;
import com.laserxprts.falcon.repository.NewsCategoryRepository;

@ExtendWith(MockitoExtension.class)
public class NewsCategoryServiceTest {

    @Mock private NewsCategoryRepository categoryRepository;
    @Mock private NewsArticleRepository articleRepository;

    @InjectMocks
    private NewsCategoryService newsCategoryService;

    private NewsCategory testCategory;

    @BeforeEach
    void setUp() {
        testCategory = new NewsCategory();
        testCategory.setId("NCAT-1");
        testCategory.setName("Tech");
    }

    @Test
    void testCreateCategory_Success() {
        when(categoryRepository.existsByNameIgnoreCase("Tech")).thenReturn(false);
        when(categoryRepository.save(any(NewsCategory.class))).thenReturn(testCategory);

        NewsCategory result = newsCategoryService.createCategory(testCategory);

        assertNotNull(result);
        assertEquals("tech", result.getSlug());
        verify(categoryRepository).save(any(NewsCategory.class));
    }

    @Test
    void testDeleteCategory_CascadesSafely() {
        NewsArticle article = new NewsArticle();
        article.setId("ART-1");
        article.setCategory("Tech");
        List<NewsArticle> articles = new ArrayList<>();
        articles.add(article);

        when(categoryRepository.findById("NCAT-1")).thenReturn(Optional.of(testCategory));
        when(articleRepository.findByCategoryIgnoreCase("Tech")).thenReturn(articles);

        newsCategoryService.deleteCategory("NCAT-1");

        assertNull(article.getCategory());
        verify(articleRepository).saveAll(articles);
        verify(categoryRepository).deleteById("NCAT-1");
    }
}
