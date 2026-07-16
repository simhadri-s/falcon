package com.laserxprts.falcon.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import lombok.Data;

import org.springframework.data.domain.Persistable;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Getter
@Setter
@Document (collection = "products")
@CompoundIndex(def = "{'name': 'text', 'description': 'text', 'category': 'text'}")
public class Product implements Persistable<String> {

    @Id
    private String id;

    @Version
    private Long version;

    @Indexed(unique = true)
    private String productCode;

    @NotBlank(message = "Name field is required")
    private String name;

    @Indexed(unique = true)
    @Pattern(regexp = "^[a-z0-9-]+$", message = "Slug must be URL-safe")
    private String slug;
    private String category;
    @Indexed
    private String categoryId;
    @Indexed
    private String subCategoryId;
    @Size(max = 2000, message = "Description cannot exceed 2000 characters")
    private String description;

    private Double mrp;
    private Double sellingPrice;
    
    // Used specifically for sorting by price where products might only have an MRP.
    private Double effectivePrice;
    
    @Valid
    private List<ProductSpec> specs;

    private List<String> imageUrls;

    @Indexed
    private boolean isFeatured;

    private Integer stockQuantity = 0;
    private boolean manageStock = false;
    private String expiryDate; // format: DD-MM-YYYY or MM-YYYY
    
    // Automated Expiry Offer Configuration
    private boolean autoOfferOnExpiry = false;
    private Integer expiryThresholdDays = 7;
    private Double expiryDiscountPercent = 10.0;
    private Double originalSellingPrice;
    @Indexed
    private boolean expiryOffer = false;

    @CreatedDate
    private LocalDateTime createdAt;

    // Ratings
    private Double averageRating = 0.0;
    private Integer reviewCount = 0;

    @Transient
    private Float score; // kept for backwards compatibility if used elsewhere

    @Indexed
    private boolean published = false;

    @Data
    public static class IndustryRef {
        private String id;
        private String name;
        private String slug;
    }

    private List<IndustryRef> industries = new ArrayList<>();

    @Transient
    private List<String> industrySlugs = new ArrayList<>();

    @Data
    public static class ProductVariant {
        private String id = UUID.randomUUID().toString();
        private String sku;
        private Map<String, String> attributes;
        private Double mrp;
        private Double sellingPrice;
        private Integer stockQuantity;
        private String imageUrl;
    }

    private boolean hasVariants = false;
    private List<ProductVariant> variants = new ArrayList<>();

    @JsonIgnore
    @Override
    public boolean isNew() {
        return id == null;
    }
}
