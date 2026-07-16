package com.laserxprts.falcon.service;

import org.springframework.stereotype.Service;

import com.laserxprts.falcon.dto.response.WishlistResponse;
import com.laserxprts.falcon.exception.ResourceNotFoundException;
import com.laserxprts.falcon.model.Product;
import com.laserxprts.falcon.model.Wishlist;
import com.laserxprts.falcon.repository.ProductRepository;
import com.laserxprts.falcon.repository.WishlistRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WishlistService {

    private final WishlistRepository wishlistRepository;
    private final ProductRepository productRepository;

    private WishlistResponse buildWishlistResponse(Wishlist wishlist) {
        if (wishlist == null) return null;

        java.util.List<String> productIds = wishlist.getProducts() != null 
            ? wishlist.getProducts().stream()
                .filter(p -> p != null)
                .map(Product::getId)
                .collect(java.util.stream.Collectors.toList())
            : java.util.List.of();

        java.util.Map<String, Product> productMap = productRepository.findAllById(productIds).stream()
            .collect(java.util.stream.Collectors.toMap(Product::getId, p -> p));

        return WishlistResponse.from(wishlist, productMap);
    }

    public WishlistResponse getWishlistByUserId(String userId) {
        Wishlist wishlist = wishlistRepository.findByUserId(userId)
            .orElseGet(() -> {
                Wishlist newWishlist = new Wishlist();
                newWishlist.setUserId(userId);
                return wishlistRepository.save(newWishlist);
            });
        return buildWishlistResponse(wishlist);
    }

    public WishlistResponse toggleWishlist(String userId, String productId) {
        Wishlist wishlist = wishlistRepository.findByUserId(userId)
            .orElseGet(() -> {
                Wishlist newWishlist = new Wishlist();
                newWishlist.setUserId(userId);
                return newWishlist;
            });

        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        boolean removed = wishlist.getProducts().removeIf(p -> p != null && p.getId().equals(productId));

        if (!removed) {
            wishlist.getProducts().add(product);
        }

        Wishlist savedWishlist = wishlistRepository.save(wishlist);
        return buildWishlistResponse(savedWishlist);
    }
}
