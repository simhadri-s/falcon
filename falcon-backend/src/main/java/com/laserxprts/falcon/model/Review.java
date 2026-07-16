package com.laserxprts.falcon.model;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Document(collection = "reviews")
@CompoundIndex(def = "{'productId': 1, 'userId': 1}", unique = true)
@CompoundIndex(def = "{'productId': 1, 'status': 1}")
public class Review {

    @Id
    private String id;

    @NotBlank(message = "Product ID is required")
    private String productId;

    @NotBlank(message = "User ID is required")
    private String userId;

    private String userName;

    @NotNull(message = "Rating is required")
    @Min(value = 1, message = "Rating must be at least 1")
    @Max(value = 5, message = "Rating cannot exceed 5")
    private Integer rating;

    @Size(max = 1000, message = "Comment cannot exceed 1000 characters")
    private String comment;

    // e.g. PENDING, APPROVED, REJECTED
    private String status = "PENDING"; 

    @CreatedDate
    private LocalDateTime createdAt;
}
