package com.laserxprts.falcon.controllers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;

import com.laserxprts.falcon.dto.request.IndustryRequest;
import com.laserxprts.falcon.dto.response.IndustryResponse;
import com.laserxprts.falcon.dto.response.ProductResponse;
import com.laserxprts.falcon.service.IndustryService;
import com.laserxprts.falcon.service.ProductService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/industries")
public class IndustryController {

    private final IndustryService industryService;
    private final ProductService productService;

    public IndustryController(IndustryService industryService, ProductService productService) {
        this.industryService = industryService;
        this.productService = productService;
    }

    // Public — anyone can browse industries and their products
    @GetMapping
    public ResponseEntity<List<IndustryResponse>> getAll() {
        return ResponseEntity.ok(industryService.getAllIndustries());
    }

    @GetMapping("/{slug}/products")
    public ResponseEntity<Map<String, Object>> getProductsByIndustry(
            @PathVariable String slug,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(value = "sortBy", required = false) String sortBy,
            @RequestParam(value = "sortDirection", required = false) String sortDirection
        ) {

        Page<ProductResponse> result = productService.getProductByIndustry(slug, page, limit, sortBy, sortDirection);

        Map<String, Object> response = new HashMap<>();
        response.put("page", page);
        response.put("data", result.getContent());
        response.put("pages", result.getTotalPages());
        response.put("total", result.getTotalElements());

        return ResponseEntity.ok(response);
    }

    // Admin only — manage industries
    @PreAuthorize("@permissionService.hasAccess('MANAGE_INDUSTRIES')")
    @PostMapping
    public ResponseEntity<IndustryResponse> create(@Valid @ModelAttribute IndustryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(industryService.createIndustry(request));
    }

    @PreAuthorize("@permissionService.hasAccess('MANAGE_INDUSTRIES')")
    @PutMapping("/{id}")
    public ResponseEntity<IndustryResponse> update(
            @PathVariable String id,
            @Valid @ModelAttribute IndustryRequest request) {
        // This also cascades the snapshot update to all products
        return ResponseEntity.ok(industryService.updateIndustry(id, request));
    }

    @PreAuthorize("@permissionService.hasAccess('MANAGE_INDUSTRIES')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        // This also removes the embedded ref from all products
        industryService.deleteIndustry(id);
        return ResponseEntity.noContent().build();
    }
}