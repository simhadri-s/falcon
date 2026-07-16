package com.laserxprts.falcon.dto.response;

import java.util.List;
import java.util.stream.Collectors;

import com.laserxprts.falcon.model.ProductSnapshot;
import com.laserxprts.falcon.model.Wishlist;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class WishlistResponse {
    private String id;
    private String userId;
    private List<ProductSnapshot> products;
    private int totalItems;

    public static WishlistResponse from(Wishlist wishlist, java.util.Map<String, com.laserxprts.falcon.model.Product> productMap) {
        if (wishlist == null) return null;

        List<ProductSnapshot> productsList = wishlist.getProducts() != null
            ? wishlist.getProducts().stream()
                .filter(p -> p != null && productMap.containsKey(p.getId()))
                .map(p -> ProductSnapshot.from(productMap.get(p.getId())))
                .collect(Collectors.toList())
            : List.of();

        return WishlistResponse.builder()
            .id(wishlist.getId())
            .userId(wishlist.getUserId())
            .products(productsList)
            .totalItems(productsList.size())
            .build();
    }
}
