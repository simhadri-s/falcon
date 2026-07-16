package com.laserxprts.falcon.controllers;

import java.security.Principal;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.laserxprts.falcon.dto.request.AddToCartRequest;
import com.laserxprts.falcon.dto.request.UpdateQuantityRequest;
import com.laserxprts.falcon.dto.response.CartResponse;
import com.laserxprts.falcon.exception.ApiException;
import com.laserxprts.falcon.service.CartService;

import lombok.RequiredArgsConstructor;

@PreAuthorize("isAuthenticated()")
@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @PostMapping
    public ResponseEntity<CartResponse> addToCart(Principal principal, @RequestBody AddToCartRequest addToCartRequest) {
        return new ResponseEntity<>(cartService.addToCart(principal.getName(), addToCartRequest), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<CartResponse> getCartByUserId(Principal principal) {
        return ResponseEntity.ok(cartService.getCartByUserId(principal.getName()));
    }

    @PutMapping
    public ResponseEntity<CartResponse> updateQuantity(
        Principal principal,
        @RequestBody UpdateQuantityRequest request
    ) {
        CartResponse response = cartService.updateQuantity(principal.getName(), request.getProductId(), request.getVariantId(), request.getQuantity());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<CartResponse> removeItem(Principal principal, @PathVariable("productId") String productId, @RequestParam(required = false) String variantId) {
        if (productId == null || productId.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Product ID cannot be null");
        }
        return ResponseEntity.ok(cartService.removeFromCart(principal.getName(), productId, variantId));
    }

    @DeleteMapping
    public ResponseEntity<CartResponse> clearCart(Principal principal) {
        return ResponseEntity.ok(cartService.clearCart(principal.getName()));
    }

    @PutMapping("/item/{productId}/active")
    public ResponseEntity<CartResponse> updateActiveStatus(
        Principal principal,
        @PathVariable("productId") String productId,
        @RequestParam(required = false) String variantId,
        @RequestBody boolean active
    ) {
        return ResponseEntity.ok(cartService.updateActiveStatus(principal.getName(), productId, variantId, active));
    }

    @PutMapping("/active")
    public ResponseEntity<CartResponse> updateAllActiveStatus(
        Principal principal,
        @RequestBody boolean active
    ) {
        return ResponseEntity.ok(cartService.updateAllActiveStatus(principal.getName(), active));
    }
}
