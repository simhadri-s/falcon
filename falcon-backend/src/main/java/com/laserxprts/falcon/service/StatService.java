package com.laserxprts.falcon.service;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.laserxprts.falcon.repository.InquiryRepository;
import com.laserxprts.falcon.repository.JobApplicationRepository;
import com.laserxprts.falcon.repository.NewsArticleRepository;
import com.laserxprts.falcon.repository.ProductRepository;
import java.util.concurrent.CompletableFuture;

@Service
public class StatService {
    private final ProductRepository productRepository;
    private final InquiryRepository inquiryRepository;
    private final NewsArticleRepository newsArticleRepository;
    private final JobApplicationRepository jobApplicationRepository;

    public StatService(ProductRepository productRepository, InquiryRepository inquiryRepository, NewsArticleRepository newsArticleRepository, JobApplicationRepository jobApplicationRepository) {
        this.productRepository = productRepository;
        this.inquiryRepository = inquiryRepository;
        this.newsArticleRepository = newsArticleRepository;
        this.jobApplicationRepository = jobApplicationRepository;
    }

    public Map<String, Long> getStats() {
        CompletableFuture<Long> productCountFuture = CompletableFuture.supplyAsync(() -> productRepository.count());
        CompletableFuture<Long> totalInquiriesFuture = CompletableFuture.supplyAsync(() -> inquiryRepository.count());
        CompletableFuture<Long> unreadInquiriesFuture = CompletableFuture.supplyAsync(() -> inquiryRepository.countByStatus("NEW"));
        CompletableFuture<Long> totalNewsFuture = CompletableFuture.supplyAsync(() -> newsArticleRepository.count());
        CompletableFuture<Long> totalApplicationsFuture = CompletableFuture.supplyAsync(() -> jobApplicationRepository.count());

        CompletableFuture.allOf(productCountFuture, totalInquiriesFuture, unreadInquiriesFuture, totalNewsFuture, totalApplicationsFuture).join();

        Map<String, Long> count = new HashMap<>();
        count.put("TotalProduct", productCountFuture.join());
        count.put("TotalInquiries", totalInquiriesFuture.join());
        count.put("UnreadInquiries", unreadInquiriesFuture.join());
        count.put("TotalNews", totalNewsFuture.join());
        count.put("TotalApplications", totalApplicationsFuture.join());

        return count;
    }
}
