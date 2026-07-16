package com.laserxprts.falcon.dto.request;

import java.util.List;
import lombok.Data;

@Data
public class CouponValidationRequest {
    private String code;
    private double orderTotal;
    private List<CouponItemRequest> items;

    @Data
    public static class CouponItemRequest {
        private String productId;
        private String categoryId;
        private double price;
        private int quantity;
    }
}
