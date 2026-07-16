package com.laserxprts.falcon.dto.request;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.laserxprts.falcon.model.Product.ProductVariant;
import com.laserxprts.falcon.model.ProductSpec;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductRequest {

    private String productCode;

    @NotBlank
    private String name;

    @NotBlank
    private String description;

    private String mrp;
    private String sellingPrice;

    private String category;
    private String categoryId;
    private String subCategoryId;
    private String specsJson;
    private List<MultipartFile> images;
    private List<String> industries;
    private boolean isFeatured;
    private boolean published;
    private Integer stockQuantity = 0;
    private boolean manageStock;
    private String expiryDate; // format: DD-MM-YYYY or MM-YYYY
    
    private boolean autoOfferOnExpiry;
    private Integer expiryThresholdDays;
    private Double expiryDiscountPercent;
    private Double originalSellingPrice;
    private boolean expiryOffer;

    private boolean hasVariants;
    private String variantsJson;

    @JsonIgnore
    public Double getMrpValue() {
        return parsePrice(mrp, "MRP");
    }

    @JsonIgnore
    public Double getSellingPriceValue() {
        return parsePrice(sellingPrice, "Selling price");
    }

    @JsonIgnore
    public boolean isMrpProvided() {
        return mrp != null;
    }

    @JsonIgnore
    public boolean isSellingPriceProvided() {
        return sellingPrice != null;
    }

    @JsonIgnore
    public List<ProductSpec> getSpecs() throws JsonProcessingException {
        if (specsJson == null || specsJson.isBlank())
            return List.of();
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(specsJson, new TypeReference<List<ProductSpec>>() {
        });
    }

    @JsonIgnore
    public List<ProductVariant> getVariantsList() throws JsonProcessingException {
        if (variantsJson == null || variantsJson.isBlank())
            return List.of();
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(variantsJson, new TypeReference<List<ProductVariant>>() {});
    }

    private Double parsePrice(String rawValue, String fieldName) {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }

        try {
            return Double.valueOf(rawValue.trim());
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(fieldName + " must be a valid number");
        }
    }
}
