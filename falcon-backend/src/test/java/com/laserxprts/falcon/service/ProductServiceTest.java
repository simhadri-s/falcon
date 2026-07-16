package com.laserxprts.falcon.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
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

import com.laserxprts.falcon.model.Cart;
import com.laserxprts.falcon.model.CartItem;
import com.laserxprts.falcon.model.Product;
import com.laserxprts.falcon.model.Wishlist;
import com.laserxprts.falcon.repository.CartRepository;
import com.laserxprts.falcon.repository.IndustryRepository;
import com.laserxprts.falcon.repository.ProductCategoryRepository;
import com.laserxprts.falcon.repository.ProductRepository;
import com.laserxprts.falcon.repository.ReviewRepository;
import com.laserxprts.falcon.repository.SubCategoryRepository;
import com.laserxprts.falcon.repository.WishlistRepository;

@ExtendWith(MockitoExtension.class)
public class ProductServiceTest {

    @Mock private ProductRepository productRepository;
    @Mock private FileUploadService fileUploadService;
    @Mock private ProductCategoryRepository productCategoryRepository;
    @Mock private SubCategoryRepository subCategoryRepository;
    @Mock private IndustryRepository industryRepository;
    @Mock private ProductCategoryService productCategoryService;
    @Mock private SubCategoryService subCategoryService;
    @Mock private CartRepository cartRepository;
    @Mock private WishlistRepository wishlistRepository;
    @Mock private ReviewRepository reviewRepository;

    @InjectMocks
    private ProductService productService;

    private Product testProduct;

    @BeforeEach
    void setUp() {
        testProduct = new Product();
        testProduct.setId("PROD-123");
        testProduct.setProductCode("P123");
        testProduct.setImageUrls(new ArrayList<>());
    }

    @Test
    void testDeleteProduct_CascadesAndCleansUpSafely() {
        // Arrange
        when(productRepository.findById("PROD-123")).thenReturn(Optional.of(testProduct));

        Cart cart = new Cart();
        cart.setId("CART-1");
        CartItem item = new CartItem();
        item.setProduct(testProduct);
        cart.setItems(new ArrayList<>(List.of(item)));

        Wishlist wishlist = new Wishlist();
        wishlist.setId("WISH-1");
        wishlist.setProducts(new ArrayList<>(List.of(testProduct)));

        when(cartRepository.findByItemsProductId("PROD-123")).thenReturn(List.of(cart));
        when(wishlistRepository.findByProductsId("PROD-123")).thenReturn(List.of(wishlist));

        // Act
        productService.deleteProduct("PROD-123");

        // Assert: Ensure images were deleted (it passes an empty list here)
        verify(fileUploadService, times(1)).deleteFiles(anyList());

        // Assert: Ensure Carts were updated properly
        verify(cartRepository, times(1)).findByItemsProductId("PROD-123");
        verify(cartRepository, times(1)).save(any());
        assertTrue(cart.getItems().isEmpty(), "Product should have been removed from cart");

        // Assert: Ensure Wishlists were updated properly
        verify(wishlistRepository, times(1)).findByProductsId("PROD-123");
        verify(wishlistRepository, times(1)).save(any());
        assertTrue(wishlist.getProducts().isEmpty(), "Product should have been removed from wishlist");

        // Assert: Ensure reviews and product itself were deleted
        verify(reviewRepository, times(1)).deleteByProductId("PROD-123");
        verify(productRepository, times(1)).deleteById("PROD-123");
    }
}
