package com.laserxprts.falcon.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.laserxprts.falcon.dto.request.CategoryRequest;
import com.laserxprts.falcon.model.Product;
import com.laserxprts.falcon.model.ProductCategory;
import com.laserxprts.falcon.repository.ProductCategoryRepository;
import com.laserxprts.falcon.repository.ProductRepository;
import com.laserxprts.falcon.repository.SubCategoryRepository;

@ExtendWith(MockitoExtension.class)
public class ProductCategoryServiceTest {

    @Mock private ProductCategoryRepository categoryRepository;
    @Mock private FileUploadService fileUploadService;
    @Mock private SubCategoryRepository subCategoryRepository;
    @Mock private ProductRepository productRepository;

    @InjectMocks
    private ProductCategoryService productCategoryService;

    private ProductCategory testCategory;

    @BeforeEach
    void setUp() {
        testCategory = new ProductCategory();
        testCategory.setId("CAT-1");
        testCategory.setName("Electronics");
        testCategory.setSlug("electronics");
    }

    @Test
    void testCreateCategory_Success() {
        CategoryRequest request = new CategoryRequest();
        request.setName("Electronics");

        when(categoryRepository.existsByNameIgnoreCase("Electronics")).thenReturn(false);
        when(categoryRepository.save(any(ProductCategory.class))).thenReturn(testCategory);

        ProductCategory result = productCategoryService.createCategory(request);

        assertNotNull(result);
        assertEquals("electronics", result.getSlug());
        verify(categoryRepository).save(any(ProductCategory.class));
    }

    @Test
    void testCreateCategory_DuplicateName_ThrowsException() {
        CategoryRequest request = new CategoryRequest();
        request.setName("Electronics");

        when(categoryRepository.existsByNameIgnoreCase("Electronics")).thenReturn(true);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> productCategoryService.createCategory(request));
        assertEquals("Product category already exists", ex.getMessage());
        verify(categoryRepository, never()).save(any(ProductCategory.class));
    }

    @Test
    void testDeleteCategory_CascadesSafely() {
        Product product = new Product();
        product.setId("PROD-1");
        product.setCategoryId("CAT-1");
        List<Product> products = new ArrayList<>();
        products.add(product);

        when(categoryRepository.findById("CAT-1")).thenReturn(Optional.of(testCategory));
        when(productRepository.findByCategoryId("CAT-1")).thenReturn(products);

        productCategoryService.deleteCategory("CAT-1");

        // Verifications
        verify(subCategoryRepository).deleteByCategoryId("CAT-1");
        verify(productRepository).saveAll(products);
        assertNull(product.getCategoryId()); // Ensure category was nullified
        verify(categoryRepository).deleteById("CAT-1");
    }
}
