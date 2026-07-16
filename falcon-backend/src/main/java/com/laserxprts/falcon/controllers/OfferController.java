package com.laserxprts.falcon.controllers;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.laserxprts.falcon.dto.response.ProductResponse;
import com.laserxprts.falcon.model.Product;
import com.laserxprts.falcon.repository.ProductRepository;
import com.laserxprts.falcon.service.ProductService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/offers")
@RequiredArgsConstructor
public class OfferController {

    private final ProductRepository productRepository;
    private final ProductService productService;

    /**
     * Public endpoint to fetch all products currently on offer.
     * This includes automated expiry offers and manual discounts.
     */
    @GetMapping
    public List<ProductResponse> getActiveOffers() {
        // Fetch all products that are currently on an automated expiry offer
        List<Product> expiryOffers = productRepository.findByExpiryOfferTrueAndPublishedTrue();
        
        // Fetch products with manual discounts (sellingPrice < mrp) that are not already in the expiry list
        List<Product> manualOffers = productRepository.findManualOffers();

        // Combine and map to response
        expiryOffers.addAll(manualOffers);
        
        return productService.enrichList(expiryOffers);
    }
}
