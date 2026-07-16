package com.laserxprts.falcon.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

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

@ExtendWith(MockitoExtension.class)
public class ReviewServiceTest {

    @Mock private ReviewRepository reviewRepository;
    @Mock private ProductRepository productRepository;
    @Mock private UserRepository userRepository;
    @Mock private OrderRepository orderRepository;

    @InjectMocks
    private ReviewService reviewService;

    private User testUser;
    private Product testProduct;
    private ReviewRequest request;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId("user-1");
        testUser.setEmail("test@test.com");

        testProduct = new Product();
        testProduct.setId("PROD-1");

        request = new ReviewRequest();
        request.setProductId("PROD-1");
        request.setRating(5);
        request.setComment("Great product");
    }

    @Test
    void testAddReview_Success() {
        when(orderRepository.existsByUserIdAndStatusIgnoreCaseAndItemsProductSnapshotId("test@test.com", "DELIVERED", "PROD-1")).thenReturn(true);
        when(productRepository.findById("PROD-1")).thenReturn(Optional.of(testProduct));
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(testUser));
        when(reviewRepository.findByProductIdAndUserId("PROD-1", "user-1")).thenReturn(Optional.empty());

        Review savedReview = new Review();
        savedReview.setId("REV-1");
        savedReview.setRating(5);
        when(reviewRepository.save(any(Review.class))).thenReturn(savedReview);

        ReviewResponse response = reviewService.addReview(request, "test@test.com");

        assertNotNull(response);
        assertEquals("REV-1", response.getId());
        assertEquals(5, response.getRating());
        verify(reviewRepository).save(any(Review.class));
        verify(productRepository).save(testProduct); // product stats should be updated
    }

    @Test
    void testAddReview_NotPurchased_ThrowsException() {
        when(orderRepository.existsByUserIdAndStatusIgnoreCaseAndItemsProductSnapshotId("test@test.com", "DELIVERED", "PROD-1")).thenReturn(false);

        ApiException ex = assertThrows(ApiException.class, () -> reviewService.addReview(request, "test@test.com"));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        assertEquals("Only customers who purchased this product can leave a review.", ex.getMessage());
        verify(reviewRepository, never()).save(any(Review.class));
    }
}
