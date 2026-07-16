package com.laserxprts.falcon.controllers;

import java.security.Principal;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.laserxprts.falcon.dto.response.WishlistResponse;
import com.laserxprts.falcon.exception.ApiException;
import com.laserxprts.falcon.service.WishlistService;

import lombok.RequiredArgsConstructor;

@PreAuthorize("isAuthenticated()")
@RestController
@RequestMapping("/api/wishlists")
@RequiredArgsConstructor
public class WishlistController {

    private final WishlistService wishlistService;

    @GetMapping
    public ResponseEntity<WishlistResponse> getWishlist(Principal principal) {
        return ResponseEntity.ok(wishlistService.getWishlistByUserId(principal.getName()));
    }

    @PostMapping("/toggle")
    public ResponseEntity<WishlistResponse> toggleWishlist(Principal principal, @RequestBody Map<String, String> request) {
        String productId = request.get("productId");
        if (productId == null || productId.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Product ID cannot be null");
        }
        return ResponseEntity.ok(wishlistService.toggleWishlist(principal.getName(), productId));
    }
}
