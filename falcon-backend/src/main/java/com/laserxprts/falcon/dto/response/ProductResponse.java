package com.laserxprts.falcon.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import com.laserxprts.falcon.model.Product;
import com.laserxprts.falcon.model.Product.IndustryRef;
import com.laserxprts.falcon.model.Product.ProductVariant;
import com.laserxprts.falcon.model.ProductSpec;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(toBuilder = true)
public class ProductResponse {
    private String id;
    private String productCode;
    private String name;
    private String slug;
    private String category;
    private String categoryId;
    private String categoryName;
    private String subCategoryId;
    private String subCategoryName;
    private String description;
    private Double mrp;
    private Double sellingPrice;
    private List<ProductSpec> specs;
    private List<String> imageUrls;
    private List<IndustryRef> industries;
    private boolean isFeatured;
    private boolean published;
    private LocalDateTime createdAt;
    private Integer stockQuantity;
    private boolean manageStock;
    private String expiryDate;
    private Double averageRating;
    private Integer reviewCount;
    
    private boolean autoOfferOnExpiry;
    private Integer expiryThresholdDays;
    private Double expiryDiscountPercent;
    private Double originalSellingPrice;
    private boolean isOnExpiryOffer;

    private boolean hasVariants;
    private List<ProductVariant> variants;

    public static ProductResponse from(Product product) {
        if (product == null) {
            return null;
        }
        
        return ProductResponse.builder()
                .id(product.getId())
                .productCode(product.getProductCode())
                .name(product.getName())
                .slug(product.getSlug())
                .category(product.getCategory())
                .categoryId(product.getCategoryId())
                .subCategoryId(product.getSubCategoryId())
                .description(product.getDescription())
                .mrp(product.getMrp())
                .sellingPrice(product.getSellingPrice())
                .specs(product.getSpecs())
                .imageUrls(product.getImageUrls())
                .industries(product.getIndustries())
                .isFeatured(product.isFeatured())
                .published(product.isPublished())
                .createdAt(product.getCreatedAt())
                .stockQuantity(product.getStockQuantity())
                .manageStock(product.isManageStock())
                .expiryDate(product.getExpiryDate())
                .averageRating(product.getAverageRating())
                .reviewCount(product.getReviewCount())
                .autoOfferOnExpiry(product.isAutoOfferOnExpiry())
                .expiryThresholdDays(product.getExpiryThresholdDays())
                .expiryDiscountPercent(product.getExpiryDiscountPercent())
                .originalSellingPrice(product.getOriginalSellingPrice())
                .isOnExpiryOffer(product.isExpiryOffer())
                .hasVariants(product.isHasVariants())
                .variants(product.getVariants())
                .build();
    }
}
