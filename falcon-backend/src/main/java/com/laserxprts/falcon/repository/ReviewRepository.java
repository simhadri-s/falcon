package com.laserxprts.falcon.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import com.laserxprts.falcon.model.Review;
import java.util.Optional;

@Repository
public interface ReviewRepository extends MongoRepository<Review, String> {

    Page<Review> findByProductIdAndStatus(String productId, String status, Pageable pageable);
    
    java.util.List<com.laserxprts.falcon.model.Review> findByProductIdAndStatus(String productId, String status);
    
    Page<Review> findByProductId(String productId, Pageable pageable);
    
    Optional<Review> findByProductIdAndUserId(String productId, String userId);
    void deleteByUserId(String userId);
    void deleteByProductId(String productId);

    @Query("{ '$or': [ { 'userName': { $regex: ?0, $options: 'i' } }, { 'comment': { $regex: ?0, $options: 'i' } } ] }")
    Page<Review> searchReviews(String keyword, Pageable pageable);
    
    @Query("{ 'status': ?1, '$or': [ { 'userName': { $regex: ?0, $options: 'i' } }, { 'comment': { $regex: ?0, $options: 'i' } } ] }")
    Page<Review> searchReviewsWithStatus(String keyword, String status, Pageable pageable);

    @Query("{ 'rating': ?1, '$or': [ { 'userName': { $regex: ?0, $options: 'i' } }, { 'comment': { $regex: ?0, $options: 'i' } } ] }")
    Page<Review> searchReviewsWithRating(String keyword, Integer rating, Pageable pageable);
    
    @Query("{ 'status': ?1, 'rating': ?2, '$or': [ { 'userName': { $regex: ?0, $options: 'i' } }, { 'comment': { $regex: ?0, $options: 'i' } } ] }")
    Page<Review> searchReviewsWithStatusAndRating(String keyword, String status, Integer rating, Pageable pageable);

    Page<Review> findByStatus(String status, Pageable pageable);
    
    Page<Review> findByRating(Integer rating, Pageable pageable);
    
    Page<Review> findByStatusAndRating(String status, Integer rating, Pageable pageable);
}
