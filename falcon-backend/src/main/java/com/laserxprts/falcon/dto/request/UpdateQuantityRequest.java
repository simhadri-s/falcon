package com.laserxprts.falcon.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateQuantityRequest {
    private String productId;
    private String variantId;
    private int quantity;
}
