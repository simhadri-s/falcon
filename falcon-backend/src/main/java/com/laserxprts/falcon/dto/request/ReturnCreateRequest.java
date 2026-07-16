package com.laserxprts.falcon.dto.request;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReturnCreateRequest {
    private String orderId;
    private List<ReturnItemRequest> items;
    private String reason;
    private String comment;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReturnItemRequest {
        private String orderItemId;
        private int quantity;
    }
}
