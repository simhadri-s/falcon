package com.laserxprts.falcon.model;

import lombok.NoArgsConstructor;

import java.util.UUID;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;

import lombok.Data;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItem {
    @Builder.Default
    private  String id = UUID.randomUUID().toString();
    private ProductSnapshot productSnapshot;
    private String variantId;
    private Map<String, String> variantAttributes;
    private int quantity;

}
