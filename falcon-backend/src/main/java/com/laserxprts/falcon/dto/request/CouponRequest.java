package com.laserxprts.falcon.dto.request;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class CouponRequest {
    private String code;
    private String discountType; // FLAT or PERCENTAGE
    private double discountValue;
    private double minOrderValue;
    private LocalDateTime expiryDate;
    private int maxUses; // 1 = single-use, 0 = unlimited
    private java.util.List<String> applicableProductIds;
    private java.util.List<String> applicableCategoryIds;
    private boolean isActive;
}
