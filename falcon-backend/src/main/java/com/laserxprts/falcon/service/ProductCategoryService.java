package com.laserxprts.falcon.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.laserxprts.falcon.dto.request.CategoryRequest;
import com.laserxprts.falcon.model.ProductCategory;
import com.laserxprts.falcon.repository.ProductCategoryRepository;
import com.laserxprts.falcon.repository.SubCategoryRepository;
import com.laserxprts.falcon.repository.ProductRepository;
import com.laserxprts.falcon.model.Product;

@Service
public class ProductCategoryService {
    
    private final ProductCategoryRepository categoryRepository;
    private final FileUploadService fileUploadService;
    private final SubCategoryRepository subCategoryRepository;
    private final ProductRepository productRepository;

    public ProductCategoryService(
            ProductCategoryRepository categoryRepository, 
            FileUploadService fileUploadService,
            SubCategoryRepository subCategoryRepository,
            ProductRepository productRepository) {
        this.categoryRepository = categoryRepository;
        this.fileUploadService = fileUploadService;
        this.subCategoryRepository = subCategoryRepository;
        this.productRepository = productRepository;
    }

    public List<ProductCategory> getAllCategories() {
        return categoryRepository.findAll();
    }

    public ProductCategory createCategory(CategoryRequest request) {
        if (request.getName() == null || request.getName().isBlank()) {
            throw new RuntimeException("Category name is required");
        }
        
        if (categoryRepository.existsByNameIgnoreCase(request.getName())) {
            throw new RuntimeException("Product category already exists");
        }

        ProductCategory category = new ProductCategory();
        category.setName(request.getName().trim());
        category.setSlug(generateSlug(category.getName()));

        if (request.getImage() != null && !request.getImage().isEmpty()) {
            String imageUrl = fileUploadService.uploadImage(request.getImage());
            category.setImageUrl(imageUrl);
        }

        return categoryRepository.save(category);
    }

    public ProductCategory updateCategory(String id, CategoryRequest request) {
        ProductCategory existing = categoryRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Category not found"));
        
        if (request.getName() != null && !request.getName().isBlank()) {
            existing.setName(request.getName().trim());
            existing.setSlug(generateSlug(existing.getName()));
        }

        if (request.getImage() != null && !request.getImage().isEmpty()) {
            String imageUrl = fileUploadService.uploadImage(request.getImage());
            existing.setImageUrl(imageUrl);
        }
        
        return categoryRepository.save(existing);
    }

    public void deleteCategory(String id) {
        ProductCategory category = categoryRepository.findById(id).orElse(null);
        if (category != null && category.getImageUrl() != null && !category.getImageUrl().isBlank()) {
            fileUploadService.deleteFiles(java.util.List.of(category.getImageUrl()));
        }

        subCategoryRepository.deleteByCategoryId(id);
        
        List<Product> products = productRepository.findByCategoryId(id);
        if (!products.isEmpty()) {
            for (Product p : products) {
                p.setCategoryId(null);
            }
            productRepository.saveAll(products);
        }

        categoryRepository.deleteById(id);
    }

    public java.util.Optional<ProductCategory> getCategoryByIdentifier(String identifier) {
        return categoryRepository.findBySlug(identifier)
            .or(() -> categoryRepository.findByNameIgnoreCase(identifier));
    }

    private String generateSlug(String title) {
        return title.toLowerCase().replaceAll("[^a-z0-9\\s]", "").trim().replaceAll("\\s+", "-");
    }
}