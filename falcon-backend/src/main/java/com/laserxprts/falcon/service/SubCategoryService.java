package com.laserxprts.falcon.service;

import java.util.List;
import org.springframework.stereotype.Service;
import com.laserxprts.falcon.model.SubCategory;
import com.laserxprts.falcon.repository.SubCategoryRepository;
import com.laserxprts.falcon.repository.ProductCategoryRepository;
import com.laserxprts.falcon.repository.ProductRepository;
import com.laserxprts.falcon.model.Product;

@Service
public class SubCategoryService {
    
    private final SubCategoryRepository subCategoryRepository;
    private final ProductCategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final FileUploadService fileUploadService;

    public SubCategoryService(SubCategoryRepository subCategoryRepository, ProductCategoryRepository categoryRepository, ProductRepository productRepository, FileUploadService fileUploadService) {
        this.subCategoryRepository = subCategoryRepository;
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.fileUploadService = fileUploadService;
    }

    public List<SubCategory> getAllSubCategories() {
        return subCategoryRepository.findAll();
    }

    public List<SubCategory> getSubCategoriesByCategory(String categoryIdentifier) {
        List<SubCategory> results = subCategoryRepository.findByCategoryId(categoryIdentifier);
        if (results.isEmpty()) {
            // Try by slug
            return categoryRepository.findBySlug(categoryIdentifier)
                .map(cat -> subCategoryRepository.findByCategoryId(cat.getId()))
                .orElseGet(() -> 
                    // Fallback to name
                    categoryRepository.findByNameIgnoreCase(categoryIdentifier)
                        .map(cat -> subCategoryRepository.findByCategoryId(cat.getId()))
                        .orElse(List.of())
                );
        }
        return results;
    }

    public SubCategory createSubCategory(SubCategory subCategory) {
        if (subCategory.getName() == null || subCategory.getName().isBlank()) {
            throw new RuntimeException("Sub-category name is required");
        }
        
        if (subCategoryRepository.existsByNameIgnoreCase(subCategory.getName())) {
            throw new RuntimeException("Sub-category already exists");
        }

        subCategory.setSlug(generateSlug(subCategory.getName()));
        return subCategoryRepository.save(subCategory);
    }

    public SubCategory updateSubCategory(String id, SubCategory updatedSubCategory) {
        SubCategory existing = subCategoryRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Sub-category not found"));
        
        if (updatedSubCategory.getName() != null && !updatedSubCategory.getName().isBlank()) {
            existing.setName(updatedSubCategory.getName());
            existing.setSlug(generateSlug(updatedSubCategory.getName()));
        }
        
        if (updatedSubCategory.getCategoryId() != null && !updatedSubCategory.getCategoryId().isBlank()) {
            existing.setCategoryId(updatedSubCategory.getCategoryId());
        }
        
        return subCategoryRepository.save(existing);
    }

    public void deleteSubCategory(String id) {
        List<Product> products = productRepository.findBySubCategoryId(id);
        if (!products.isEmpty()) {
            for (Product p : products) {
                p.setSubCategoryId(null);
            }
            productRepository.saveAll(products);
        }

        subCategoryRepository.deleteById(id);
    }

    public java.util.Optional<SubCategory> getSubCategoryByIdentifier(String identifier) {
        return subCategoryRepository.findBySlug(identifier)
            .or(() -> subCategoryRepository.findByNameIgnoreCase(identifier));
    }

    private String generateSlug(String title) {
        return title.toLowerCase().replaceAll("[^a-z0-9\\s]", "").trim().replaceAll("\\s+", "-");
    }
}
