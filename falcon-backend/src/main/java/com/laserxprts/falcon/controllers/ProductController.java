package com.laserxprts.falcon.controllers;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.laserxprts.falcon.dto.request.ProductRequest;
import com.laserxprts.falcon.dto.response.ProductResponse;
import com.laserxprts.falcon.exception.ApiException;
import com.laserxprts.falcon.model.Product;
import com.laserxprts.falcon.security.PermissionService;
import com.laserxprts.falcon.service.ProductService;

import jakarta.validation.Valid;

@PreAuthorize("@permissionService.hasAccess('MANAGE_PRODUCTS')")
@RestController
@RequestMapping("/api/products")
public class ProductController {
    
    private final ProductService productService;
    private final PermissionService permissionService;

    public ProductController(ProductService productService, PermissionService permissionService) {
        this.productService = productService;
        this.permissionService = permissionService;
    }

    @PreAuthorize("permitAll()")
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllProducts(
        @RequestParam(value = "category", required = false) String category,
        @RequestParam(value = "subCategory", required = false) String subCategory,
        @RequestParam(value = "search", required = false) String keyword,
        @RequestParam(value = "page", defaultValue = "1") int page,
        @RequestParam(value = "limit", defaultValue = "10") int limit,
        @RequestParam(value = "sortBy", required = false) String sortBy,
        @RequestParam(value = "sortDirection", required = false) String sortDirection
    ) {
        boolean canViewDrafts = permissionService.hasAccess("UPDATE_PRODUCT");
        Page<ProductResponse> products = productService.getAllProducts(category, subCategory, keyword, page, limit, sortBy, sortDirection, canViewDrafts);

        Map<String, Object> response = new HashMap<>();
        response.put("data", products.getContent());
        response.put("total", products.getTotalElements());
        response.put("page", page);
        response.put("pages", products.getTotalPages());

        return ResponseEntity.ok(response);
    }

    @PreAuthorize("permitAll()")
    @GetMapping("/featured")
    public ResponseEntity<Map<String, Object>> getFeaturedProducts(
        @RequestParam(value = "page", defaultValue = "1") int page,
        @RequestParam(value = "limit", defaultValue = "10") int limit
    ) {  
        Page<ProductResponse> products = productService.getFeaturedProducts(page, limit, isAdmin());

        Map<String, Object> response = new HashMap<>();
        response.put("data", products.getContent());
        response.put("total", products.getTotalElements());
        response.put("page", page);
        response.put("pages", products.getTotalPages());

        return ResponseEntity.ok(response);
    }

    @PreAuthorize("permitAll()")
    @GetMapping("/{slug}")
    public ProductResponse getProductBySlug(@PathVariable String slug) {
        return productService.getProductBySlug(slug);
    }

    @PreAuthorize("permitAll()")
    @GetMapping("/id/{id}")
    public ProductResponse getProductById(@PathVariable String id) {
        return productService.getProductById(id);
    }

    @PostMapping
    public ResponseEntity<ProductResponse> createProduct(@Valid @ModelAttribute ProductRequest productRequest) {
    Product product = new Product();
    product.setProductCode(productRequest.getProductCode());
    product.setName(productRequest.getName());
    product.setCategory(productRequest.getCategory());
    product.setCategoryId(productRequest.getCategoryId());
    product.setSubCategoryId(productRequest.getSubCategoryId());
    product.setDescription(productRequest.getDescription());
    product.setMrp(productRequest.getMrpValue());
    product.setSellingPrice(productRequest.getSellingPriceValue());
    product.setIndustrySlugs(productRequest.getIndustries()); // FIXED: was setIndustries()
    product.setStockQuantity(productRequest.getStockQuantity());
    product.setManageStock(productRequest.isManageStock());

    try {
        product.setSpecs(productRequest.getSpecs());
    } catch (JsonProcessingException e) {
        throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid product specs JSON");
    }
    try {
        product.setVariants(productRequest.getVariantsList());
    } catch (JsonProcessingException e) {
        throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid product variants JSON");
    }
    product.setHasVariants(productRequest.isHasVariants());
    product.setFeatured(productRequest.isFeatured());
    product.setPublished(productRequest.isPublished());
    product.setExpiryDate(productRequest.getExpiryDate());
    product.setAutoOfferOnExpiry(productRequest.isAutoOfferOnExpiry());
    product.setExpiryThresholdDays(productRequest.getExpiryThresholdDays());
    product.setExpiryDiscountPercent(productRequest.getExpiryDiscountPercent());

    ProductResponse savedProduct = productService.createProduct(product, productRequest.getImages());
    return new ResponseEntity<>(savedProduct, HttpStatus.CREATED);
}

    @PutMapping("/{id}")
    public ResponseEntity<ProductResponse> updateProduct(
        @PathVariable String id,
        @ModelAttribute ProductRequest productRequest
    ) {
        Product updatedProduct = new Product();
        updatedProduct.setProductCode(productRequest.getProductCode());
        updatedProduct.setName(productRequest.getName());
        updatedProduct.setCategory(productRequest.getCategory());
        updatedProduct.setCategoryId(productRequest.getCategoryId());
        updatedProduct.setSubCategoryId(productRequest.getSubCategoryId());
        updatedProduct.setDescription(productRequest.getDescription());
        updatedProduct.setMrp(productRequest.getMrpValue());
        updatedProduct.setSellingPrice(productRequest.getSellingPriceValue());
        updatedProduct.setExpiryDiscountPercent(productRequest.getExpiryDiscountPercent());
        updatedProduct.setExpiryOffer(productRequest.isExpiryOffer());
        updatedProduct.setOriginalSellingPrice(productRequest.getOriginalSellingPrice());
        updatedProduct.setFeatured(productRequest.isFeatured());
        updatedProduct.setIndustrySlugs(productRequest.getIndustries());
        updatedProduct.setPublished(productRequest.isPublished());
        updatedProduct.setStockQuantity(productRequest.getStockQuantity());
        updatedProduct.setManageStock(productRequest.isManageStock());
        updatedProduct.setExpiryDate(productRequest.getExpiryDate());
        updatedProduct.setAutoOfferOnExpiry(productRequest.isAutoOfferOnExpiry());
        updatedProduct.setExpiryThresholdDays(productRequest.getExpiryThresholdDays());
        updatedProduct.setExpiryDiscountPercent(productRequest.getExpiryDiscountPercent());
        try {
            updatedProduct.setSpecs(productRequest.getSpecs());
        } catch (JsonProcessingException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid product specs JSON");
        }
        try {
            updatedProduct.setVariants(productRequest.getVariantsList());
        } catch (JsonProcessingException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid product variants JSON");
        }
        updatedProduct.setHasVariants(productRequest.isHasVariants());

        ProductResponse savedProduct = productService.updateProduct(
            id,
            updatedProduct,
            productRequest.getImages(),
            productRequest.isMrpProvided(),
            productRequest.isSellingPriceProvided()
        );
        return new ResponseEntity<>(savedProduct, HttpStatus.ACCEPTED);
    }

    @PatchMapping("/{id}/stock-and-price")
    public ResponseEntity<ProductResponse> updateStockAndPrice(
        @PathVariable String id,
        @RequestBody Map<String, Object> updates
    ) {
        ProductResponse response = productService.updateStockAndPrice(id, updates);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/toggle-publish")
    public ResponseEntity<ProductResponse> toggleProductPublishStatus(@PathVariable String id) {
        return ResponseEntity.ok(productService.togglePublishStatus(id));
    }

    @PatchMapping("/{id}/toggle-featured")
    public ResponseEntity<ProductResponse> toggleProductFeaturedStatus(@PathVariable String id) {
        return ResponseEntity.ok(productService.toggleFeaturedStatus(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteProduct(@PathVariable String id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }

    private boolean isAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal().equals("anonymousUser")) {
            return false;
        }
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ALL_PERMISSIONS"));
    }

    @PostMapping("/bulk-upload")
    public ResponseEntity<Map<String, Object>> bulkUploadProducts(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "File is empty");
        }
        // Ensure the file is actually a CSV
        String contentType = file.getContentType();
        String originalFilename = Objects.toString(file.getOriginalFilename(), "");
        if (!"text/csv".equalsIgnoreCase(contentType) && !originalFilename.toLowerCase().endsWith(".csv")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Please upload a valid CSV file.");
        }

        Map<String, Object> uploadReport = productService.bulkUploadProducts(file);
        
        return ResponseEntity.ok(uploadReport);
    }
}
