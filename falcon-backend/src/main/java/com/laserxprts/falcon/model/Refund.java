package com.laserxprts.falcon.model;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import com.laserxprts.falcon.enums.RefundMethod;
import com.laserxprts.falcon.enums.RefundStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "refunds")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Refund {
    @Id
    private String id;
    private String returnRequestId;
    private String orderId;
    private String userId;
    private double amount;
    private RefundStatus status;
    private RefundMethod method;
    private String transactionId;
    private String notes;

    @CreatedDate
    private LocalDateTime createdAt;
    @LastModifiedDate
    private LocalDateTime updatedAt;
}
