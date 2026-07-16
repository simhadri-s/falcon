package com.laserxprts.falcon.dto.response;

import java.util.Map;

import com.laserxprts.falcon.model.CartItem;
import com.laserxprts.falcon.model.Product.ProductVariant;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class CartItemResponse {
    private String id;
    private String productId;
    private String productName;
    private String productSlug;
    private Double mrp;
    private Double sellingPrice;
    private int quantity;
    private boolean active;
    
    private String variantId;
    private Map<String, String> variantAttributes;
    private String imageUrl;

    public static CartItemResponse from(CartItem item, com.laserxprts.falcon.model.Product product) {
        if (item == null || product == null) return null;

        Double mrp = product.getMrp();
        Double sellingPrice = product.getSellingPrice();
        String imageUrl = product.getImageUrls() != null && !product.getImageUrls().isEmpty() 
            ? product.getImageUrls().get(0) : null;
        Map<String, String> variantAttributes = null;

        if (product.isHasVariants() && item.getVariantId() != null) {
            ProductVariant variant = product.getVariants().stream()
                .filter(v -> v.getId().equals(item.getVariantId()))
                .findFirst()
                .orElse(null);
            
            if (variant != null) {
                mrp = variant.getMrp() != null ? variant.getMrp() : mrp;
                sellingPrice = variant.getSellingPrice() != null ? variant.getSellingPrice() : sellingPrice;
                variantAttributes = variant.getAttributes();
                imageUrl = variant.getImageUrl() != null ? variant.getImageUrl() : imageUrl;
            }
        }

        return CartItemResponse.builder()
        .id(item.getId())
        .productId(product.getId())
        .productName(product.getName())
        .productSlug(product.getSlug())
        .mrp(mrp)
        .sellingPrice(sellingPrice)
        .quantity(item.getQuantity())
        .active(item.isActive())
        .variantId(item.getVariantId())
        .variantAttributes(variantAttributes)
        .imageUrl(imageUrl)
        .build();
    }
}
