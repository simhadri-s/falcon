package com.laserxprts.falcon.dto.request;

import lombok.Data;

@Data
public class OrderItemRequest {
    private String productId;
    private String variantId;
    private int quantity;
}