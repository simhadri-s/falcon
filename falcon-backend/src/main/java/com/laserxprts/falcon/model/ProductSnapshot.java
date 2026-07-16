package com.laserxprts.falcon.model;

import java.util.List;
import java.util.Map;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class ProductSnapshot {
    private String id;
    private String productCode;
    private String name;
    private String slug;
    private String description;
    private String categoryId;
    private Double mrp;
    private Double sellingPrice;
    private List<ProductSpec> specs;
    private List<String> imageUrls;
    
    private String variantId;
    private Map<String, String> variantAttributes;

    public static ProductSnapshot from(Product product) {
        return ProductSnapshot.builder()
            .id(product.getId())
            .productCode(product.getProductCode())
            .name(product.getName())
            .slug(product.getSlug())
            .description(product.getDescription())
            .categoryId(product.getCategoryId())
            .mrp(product.getMrp())
            .sellingPrice(product.getSellingPrice())
            .specs(product.getSpecs())
            .imageUrls(product.getImageUrls())
            .build();
    }
}
