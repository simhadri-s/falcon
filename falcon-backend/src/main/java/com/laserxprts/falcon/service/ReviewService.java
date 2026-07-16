package com.laserxprts.falcon.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.laserxprts.falcon.dto.request.ReviewRequest;
import com.laserxprts.falcon.dto.response.ReviewResponse;
import com.laserxprts.falcon.exception.ApiException;
import com.laserxprts.falcon.model.Product;
import com.laserxprts.falcon.model.Review;
import com.laserxprts.falcon.model.User;
import com.laserxprts.falcon.repository.OrderRepository;
import com.laserxprts.falcon.repository.ProductRepository;
import com.laserxprts.falcon.repository.ReviewRepository;
import com.laserxprts.falcon.repository.UserRepository;

@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;

    public ReviewService(ReviewRepository reviewRepository, ProductRepository productRepository, UserRepository userRepository, OrderRepository orderRepository) {
        this.reviewRepository = reviewRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
    }

    public boolean canUserReviewProduct(String productId, String userEmail) {
        // Find user to get their ID if needed, but Order stores userEmail in userId field based on OrderService.java:123
        // OrderService.java:123 says .userId(userEmail)
        
        return orderRepository.existsByUserIdAndStatusIgnoreCaseAndItemsProductSnapshotId(userEmail, "DELIVERED", productId);
    }

    public ReviewResponse addReview(ReviewRequest request, String userEmail) {
        if (!canUserReviewProduct(request.getProductId(), userEmail)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only customers who purchased this product can leave a review.");
        }

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Product not found"));

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));

        if (reviewRepository.findByProductIdAndUserId(product.getId(), user.getId()).isPresent()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "You have already reviewed this product.");
        }

        Review review = new Review();
        review.setProductId(product.getId());
        review.setUserId(user.getId());
        review.setUserName(user.getName());
        review.setRating(request.getRating());
        review.setComment(request.getComment());
        review.setCreatedAt(LocalDateTime.now());
        // New reviews start as PENDING — admin must approve before they appear publicly.
        review.setStatus("PENDING");

        Review savedReview = reviewRepository.save(review);
        updateProductRating(product.getId());

        return ReviewResponse.from(savedReview);
    }

    public ReviewResponse getUserReviewForProduct(String productId, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));

        return reviewRepository.findByProductIdAndUserId(productId, user.getId())
                .map(ReviewResponse::from)
                .orElse(null);
    }

    public ReviewResponse updateReview(ReviewRequest request, String userEmail) {
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Product not found"));

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));

        Review review = reviewRepository.findByProductIdAndUserId(product.getId(), user.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Review not found"));

        review.setRating(request.getRating());
        review.setComment(request.getComment());
        // Reset to PENDING on edit — admin must re-approve the updated content.
        review.setStatus("PENDING");

        Review savedReview = reviewRepository.save(review);
        updateProductRating(product.getId());

        return ReviewResponse.from(savedReview);
    }

    public Page<ReviewResponse> getReviewsForProduct(String productId, int page, int limit) {
        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), limit, Sort.by(Sort.Direction.DESC, "createdAt"));
        return reviewRepository.findByProductIdAndStatus(productId, "APPROVED", pageable)
                .map(ReviewResponse::from);
    }

    // Admin Methods
    public Page<ReviewResponse> getAllReviews(String keyword, String status, Integer rating, int page, int limit, String sortBy, String sortDirection) {
        String sortField = (sortBy != null && !sortBy.isEmpty()) ? sortBy : "createdAt";
        Sort.Direction direction = (sortDirection != null && sortDirection.equalsIgnoreCase("asc")) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), limit, Sort.by(direction, sortField));

        Page<Review> reviews;
        boolean hasKeyword = keyword != null && !keyword.isBlank();
        boolean hasStatus = status != null && !status.isBlank();
        boolean hasRating = rating != null && rating > 0;

        if (hasKeyword) {
            if (hasStatus && hasRating) {
                reviews = reviewRepository.searchReviewsWithStatusAndRating(keyword, status, rating, pageable);
            } else if (hasStatus) {
                reviews = reviewRepository.searchReviewsWithStatus(keyword, status, pageable);
            } else if (hasRating) {
                reviews = reviewRepository.searchReviewsWithRating(keyword, rating, pageable);
            } else {
                reviews = reviewRepository.searchReviews(keyword, pageable);
            }
        } else {
            if (hasStatus && hasRating) {
                reviews = reviewRepository.findByStatusAndRating(status, rating, pageable);
            } else if (hasStatus) {
                reviews = reviewRepository.findByStatus(status, pageable);
            } else if (hasRating) {
                reviews = reviewRepository.findByRating(rating, pageable);
            } else {
                reviews = reviewRepository.findAll(pageable);
            }
        }

        java.util.List<String> productIds = reviews.stream().map(Review::getProductId).distinct().collect(java.util.stream.Collectors.toList());
        java.util.Map<String, Product> productMap = new java.util.HashMap<>();
        productRepository.findAllById(productIds).forEach(p -> productMap.put(p.getId(), p));

        return reviews.map(review -> {
            ReviewResponse res = ReviewResponse.from(review);
            Product p = productMap.get(review.getProductId());
            if (p != null) {
                res.setProductCode(p.getProductCode());
                res.setProductSlug(p.getSlug());
            }
            return res;
        });
    }

    public void deleteReview(String id) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Review not found"));
        reviewRepository.delete(review);
        updateProductRating(review.getProductId());
    }

    public ReviewResponse updateReviewStatus(String id, String status) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Review not found"));
        
        if (!status.equals("APPROVED") && !status.equals("REJECTED") && !status.equals("PENDING")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid status");
        }

        review.setStatus(status);
        Review saved = reviewRepository.save(review);
        updateProductRating(review.getProductId());
        
        return ReviewResponse.from(saved);
    }

    private void updateProductRating(String productId) {
        Product product = productRepository.findById(productId).orElse(null);
        if (product == null) return;

        List<Review> reviewsList = reviewRepository.findByProductIdAndStatus(productId, "APPROVED");
        
        System.out.println("Updating rating for product: " + productId);
        System.out.println("Found " + reviewsList.size() + " approved reviews");

        if (reviewsList.isEmpty()) {
            product.setAverageRating(0.0);
            product.setReviewCount(0);
        } else {
            double avg = reviewsList.stream()
                    .mapToInt(Review::getRating)
                    .average()
                    .orElse(0.0);
            product.setAverageRating(avg);
            product.setReviewCount(reviewsList.size());
            System.out.println("New average rating: " + avg + ", count: " + reviewsList.size());
        }
        productRepository.save(product);
    }
}
