package com.laserxprts.falcon.dto.response;

import java.util.List;
import java.util.stream.Collectors;

import com.laserxprts.falcon.model.Cart;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(toBuilder = true)
public class CartResponse {
    private String id;
    private String userId;
    private List<CartItemResponse> items;
    private int totalItems;
    private double subtotal;
    private double totalDiscount;
    private double totalAmount;

    public static CartResponse from(Cart cart, java.util.Map<String, com.laserxprts.falcon.model.Product> productMap) {
        if (cart == null) return null;

        List<CartItemResponse> itemResponse = cart.getItems() != null
            ? cart.getItems().stream()
                .filter(item -> item.getProduct() != null && productMap.containsKey(item.getProduct().getId()))
                .map(item -> CartItemResponse.from(item, productMap.get(item.getProduct().getId())))
                .collect(Collectors.toList())
            : List.of();

        return CartResponse.builder()
            .id(cart.getId())
            .userId(cart.getUserId())
            .items(itemResponse)
            .totalItems(itemResponse.stream().filter(CartItemResponse::isActive).mapToInt(CartItemResponse::getQuantity).sum())
            .build();
    }
}