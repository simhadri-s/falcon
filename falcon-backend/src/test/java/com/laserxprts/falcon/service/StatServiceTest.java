package com.laserxprts.falcon.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.laserxprts.falcon.repository.InquiryRepository;
import com.laserxprts.falcon.repository.JobApplicationRepository;
import com.laserxprts.falcon.repository.NewsArticleRepository;
import com.laserxprts.falcon.repository.ProductRepository;

@ExtendWith(MockitoExtension.class)
public class StatServiceTest {

    @Mock private ProductRepository productRepository;
    @Mock private InquiryRepository inquiryRepository;
    @Mock private NewsArticleRepository newsArticleRepository;
    @Mock private JobApplicationRepository jobApplicationRepository;

    @InjectMocks
    private StatService statService;

    @Test
    void testGetStats() {
        when(productRepository.count()).thenReturn(100L);
        when(inquiryRepository.count()).thenReturn(50L);
        when(inquiryRepository.countByStatus("NEW")).thenReturn(5L);
        when(newsArticleRepository.count()).thenReturn(20L);
        when(jobApplicationRepository.count()).thenReturn(10L);

        Map<String, Long> stats = statService.getStats();

        assertNotNull(stats);
        assertEquals(100L, stats.get("TotalProduct"));
        assertEquals(50L, stats.get("TotalInquiries"));
        assertEquals(5L, stats.get("UnreadInquiries"));
        assertEquals(20L, stats.get("TotalNews"));
        assertEquals(10L, stats.get("TotalApplications"));
    }
}
