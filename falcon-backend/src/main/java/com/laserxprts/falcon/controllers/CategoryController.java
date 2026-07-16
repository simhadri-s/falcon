package com.laserxprts.falcon.controllers;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.laserxprts.falcon.model.NewsCategory;
import com.laserxprts.falcon.model.ProductCategory;
import com.laserxprts.falcon.model.SubCategory;
import com.laserxprts.falcon.service.NewsCategoryService;
import com.laserxprts.falcon.service.ProductCategoryService;
import com.laserxprts.falcon.service.SubCategoryService;
import com.laserxprts.falcon.dto.request.CategoryRequest;

@PreAuthorize("@permissionService.hasAccess('MANAGE_CATEGORIES')")
@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final ProductCategoryService productCategoryService;
    private final NewsCategoryService newsCategoryService;
    private final SubCategoryService subCategoryService;

    public CategoryController(ProductCategoryService productCategoryService, NewsCategoryService newsCategoryService, SubCategoryService subCategoryService) {
        this.productCategoryService = productCategoryService;
        this.newsCategoryService = newsCategoryService;
        this.subCategoryService = subCategoryService;
    }

    // --- PRODUCT CATEGORIES ---

    @PreAuthorize("permitAll()")
    @GetMapping("/products")
    public ResponseEntity<List<ProductCategory>> getAllProductCategories() {
        return ResponseEntity.ok(productCategoryService.getAllCategories());
    }

    @PostMapping("/products")
    public ResponseEntity<ProductCategory> createProductCategory(@ModelAttribute CategoryRequest request) {
        return ResponseEntity.ok(productCategoryService.createCategory(request));
    }

    @PutMapping("/products/{id}")
    public ResponseEntity<ProductCategory> updateProductCategory(@PathVariable String id, @ModelAttribute CategoryRequest request) {
        return ResponseEntity.ok(productCategoryService.updateCategory(id, request));
    }

    @DeleteMapping("/products/{id}")
    public ResponseEntity<?> deleteProductCategory(@PathVariable String id) {
        productCategoryService.deleteCategory(id);
        return ResponseEntity.ok().build();
    }

    // --- SUB CATEGORIES ---

    @PreAuthorize("permitAll()")
    @GetMapping("/sub")
    public ResponseEntity<List<SubCategory>> getAllSubCategories() {
        return ResponseEntity.ok(subCategoryService.getAllSubCategories());
    }

    @PreAuthorize("permitAll()")
    @GetMapping("/sub/category/{categoryId}")
    public ResponseEntity<List<SubCategory>> getSubCategoriesByCategory(@PathVariable String categoryId) {
        return ResponseEntity.ok(subCategoryService.getSubCategoriesByCategory(categoryId));
    }

    @PostMapping("/sub")
    public ResponseEntity<SubCategory> createSubCategory(@RequestBody SubCategory subCategory) {
        return ResponseEntity.ok(subCategoryService.createSubCategory(subCategory));
    }

    @PutMapping("/sub/{id}")
    public ResponseEntity<SubCategory> updateSubCategory(@PathVariable String id, @RequestBody SubCategory subCategory) {
        return ResponseEntity.ok(subCategoryService.updateSubCategory(id, subCategory));
    }

    @DeleteMapping("/sub/{id}")
    public ResponseEntity<?> deleteSubCategory(@PathVariable String id) {
        subCategoryService.deleteSubCategory(id);
        return ResponseEntity.ok().build();
    }

    // --- NEWS CATEGORIES ---


    @PreAuthorize("permitAll()")
    @GetMapping("/news")
    public ResponseEntity<List<NewsCategory>> getAllNewsCategories() {
        return ResponseEntity.ok(newsCategoryService.getAllCategories());
    }

    @PostMapping("/news")
    public ResponseEntity<NewsCategory> createNewsCategory(@RequestBody NewsCategory category) {
        return ResponseEntity.ok(newsCategoryService.createCategory(category));
    }

    @DeleteMapping("/news/{id}")
    public ResponseEntity<?> deleteNewsCategory(@PathVariable String id) {
        newsCategoryService.deleteCategory(id);
        return ResponseEntity.ok().build();
    }
}