package com.laserxprts.falcon.dto.request;

import java.time.LocalDateTime;
import java.util.List;

import com.laserxprts.falcon.enums.DiscountType;
import com.laserxprts.falcon.enums.OfferType;

import lombok.Data;

@Data
public class OfferRequest {
    private String name;
    private String description;
    private OfferType type;
    private DiscountType discountType;
    private double discountValue;
    private double minOrderValue;
    private List<String> productIds;
    private List<String> categoryIds;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private boolean isActive;
}
