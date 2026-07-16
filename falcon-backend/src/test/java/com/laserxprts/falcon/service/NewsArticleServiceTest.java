package com.laserxprts.falcon.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Optional;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.laserxprts.falcon.model.NewsArticle;
import com.laserxprts.falcon.repository.NewsArticleRepository;
import com.laserxprts.falcon.repository.NewsCategoryRepository;

@ExtendWith(MockitoExtension.class)
public class NewsArticleServiceTest {

    @Mock private NewsArticleRepository newsArticleRepository;
    @Mock private NewsCategoryRepository newsCategoryRepository;
    @Mock private FileUploadService fileUploadService;

    @InjectMocks
    private NewsArticleService newsArticleService;

    private NewsArticle testArticle;

    @BeforeEach
    void setUp() {
        testArticle = new NewsArticle();
        testArticle.setId("NEWS-1");
        testArticle.setTitle("Breaking News");
        testArticle.setSlug("breaking-news");
    }

    @Test
    void testDeleteNews_Success() {
        when(newsArticleRepository.findById("NEWS-1")).thenReturn(Optional.of(testArticle));

        newsArticleService.deleteNews("NEWS-1");

        verify(newsArticleRepository).deleteById("NEWS-1");
    }
}
