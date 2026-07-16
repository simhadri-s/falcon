package com.laserxprts.falcon.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.laserxprts.falcon.model.Product;
import com.laserxprts.falcon.model.SubCategory;
import com.laserxprts.falcon.repository.ProductCategoryRepository;
import com.laserxprts.falcon.repository.ProductRepository;
import com.laserxprts.falcon.repository.SubCategoryRepository;

@ExtendWith(MockitoExtension.class)
public class SubCategoryServiceTest {

    @Mock private SubCategoryRepository subCategoryRepository;
    @Mock private ProductCategoryRepository categoryRepository;
    @Mock private ProductRepository productRepository;
    @Mock private FileUploadService fileUploadService;

    @InjectMocks
    private SubCategoryService subCategoryService;

    private SubCategory testSubCategory;

    @BeforeEach
    void setUp() {
        testSubCategory = new SubCategory();
        testSubCategory.setId("SUBCAT-1");
        testSubCategory.setName("Laptops");
        testSubCategory.setSlug("laptops");
        testSubCategory.setCategoryId("CAT-1");
    }

    @Test
    void testCreateSubCategory_Success() {
        when(subCategoryRepository.existsByNameIgnoreCase("Laptops")).thenReturn(false);
        when(subCategoryRepository.save(any(SubCategory.class))).thenReturn(testSubCategory);

        SubCategory newSubCat = new SubCategory();
        newSubCat.setName("Laptops");

        SubCategory result = subCategoryService.createSubCategory(newSubCat);

        assertNotNull(result);
        assertEquals("laptops", result.getSlug());
        verify(subCategoryRepository).save(any(SubCategory.class));
    }

    @Test
    void testCreateSubCategory_DuplicateName_ThrowsException() {
        when(subCategoryRepository.existsByNameIgnoreCase("Laptops")).thenReturn(true);

        SubCategory newSubCat = new SubCategory();
        newSubCat.setName("Laptops");

        RuntimeException ex = assertThrows(RuntimeException.class, () -> subCategoryService.createSubCategory(newSubCat));
        assertEquals("Sub-category already exists", ex.getMessage());
        verify(subCategoryRepository, never()).save(any(SubCategory.class));
    }

    @Test
    void testDeleteSubCategory_CascadesSafely() {
        Product product = new Product();
        product.setId("PROD-1");
        product.setSubCategoryId("SUBCAT-1");
        List<Product> products = new ArrayList<>();
        products.add(product);

        when(productRepository.findBySubCategoryId("SUBCAT-1")).thenReturn(products);

        subCategoryService.deleteSubCategory("SUBCAT-1");

        verify(productRepository).saveAll(products);
        assertNull(product.getSubCategoryId()); // Ensure subcategory was nullified
        verify(subCategoryRepository).deleteById("SUBCAT-1");
    }
}
