package com.laserxprts.falcon.dto.response;

import java.time.LocalDateTime;
import com.laserxprts.falcon.model.Review;
import lombok.Data;

@Data
public class ReviewResponse {
    private String id;
    private String productId;
    private String productCode;
    private String productSlug;
    private String userId;
    private String userName;
    private Integer rating;
    private String comment;
    private String status;
    private LocalDateTime createdAt;

    public static ReviewResponse from(Review review) {
        ReviewResponse response = new ReviewResponse();
        response.setId(review.getId());
        response.setProductId(review.getProductId());
        response.setUserId(review.getUserId());
        response.setUserName(review.getUserName());
        response.setRating(review.getRating());
        response.setComment(review.getComment());
        response.setStatus(review.getStatus());
        response.setCreatedAt(review.getCreatedAt());
        return response;
    }
}
