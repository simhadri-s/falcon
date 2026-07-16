package com.laserxprts.falcon.controllers;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.laserxprts.falcon.dto.request.ReviewRequest;
import com.laserxprts.falcon.dto.response.ReviewResponse;
import com.laserxprts.falcon.service.ReviewService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    // Public API
    @PreAuthorize("permitAll()")
    @GetMapping("/product/{productId}")
    public ResponseEntity<Map<String, Object>> getProductReviews(
        @PathVariable String productId,
        @RequestParam(value = "page", defaultValue = "1") int page,
        @RequestParam(value = "limit", defaultValue = "10") int limit
    ) {
        Page<ReviewResponse> reviews = reviewService.getReviewsForProduct(productId, page, limit);
        Map<String, Object> response = new HashMap<>();
        response.put("data", reviews.getContent());
        response.put("total", reviews.getTotalElements());
        response.put("page", page);
        response.put("pages", reviews.getTotalPages());
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/check-eligibility/{productId}")
    public ResponseEntity<Map<String, Boolean>> checkReviewEligibility(Principal principal, @PathVariable String productId) {
        String userEmail = principal.getName();
        boolean isEligible = reviewService.canUserReviewProduct(productId, userEmail);
        Map<String, Boolean> response = new HashMap<>();
        response.put("eligible", isEligible);
        return ResponseEntity.ok(response);
    }

    // Authenticated User API
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/my/{productId}")
    public ResponseEntity<ReviewResponse> getMyReview(Principal principal, @PathVariable String productId) {
        String userEmail = principal.getName();
        ReviewResponse review = reviewService.getUserReviewForProduct(productId, userEmail);
        if (review == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(review);
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping
    public ResponseEntity<ReviewResponse> addReview(Principal principal, @Valid @RequestBody ReviewRequest request) {
        String userEmail = principal.getName();
        ReviewResponse savedReview = reviewService.addReview(request, userEmail);
        return new ResponseEntity<>(savedReview, HttpStatus.CREATED);
    }

    @PreAuthorize("isAuthenticated()")
    @PutMapping
    public ResponseEntity<ReviewResponse> updateReview(Principal principal, @Valid @RequestBody ReviewRequest request) {
        String userEmail = principal.getName();
        ReviewResponse updatedReview = reviewService.updateReview(request, userEmail);
        return ResponseEntity.ok(updatedReview);
    }

    // Admin APIs
    @PreAuthorize("@permissionService.hasAccess('MANAGE_REVIEWS')")
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllReviews(
        @RequestParam(value = "search", required = false) String keyword,
        @RequestParam(value = "status", required = false) String status,
        @RequestParam(value = "rating", required = false) Integer rating,
        @RequestParam(value = "page", defaultValue = "1") int page,
        @RequestParam(value = "limit", defaultValue = "10") int limit,
        @RequestParam(value = "sortBy", required = false) String sortBy,
        @RequestParam(value = "sortDirection", required = false) String sortDirection
    ) {
        Page<ReviewResponse> reviews = reviewService.getAllReviews(keyword, status, rating, page, limit, sortBy, sortDirection);
        Map<String, Object> response = new HashMap<>();
        response.put("data", reviews.getContent());
        response.put("total", reviews.getTotalElements());
        response.put("page", page);
        response.put("pages", reviews.getTotalPages());
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("@permissionService.hasAccess('MANAGE_REVIEWS')")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteReview(@PathVariable String id) {
        reviewService.deleteReview(id);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("@permissionService.hasAccess('MANAGE_REVIEWS')")
    @PatchMapping("/{id}/status")
    public ResponseEntity<ReviewResponse> updateReviewStatus(
        @PathVariable String id,
        @RequestBody Map<String, String> payload
    ) {
        String status = payload.get("status");
        return ResponseEntity.ok(reviewService.updateReviewStatus(id, status));
    }
}

