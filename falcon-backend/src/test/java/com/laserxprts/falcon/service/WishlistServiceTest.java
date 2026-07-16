package com.laserxprts.falcon.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.laserxprts.falcon.dto.response.WishlistResponse;
import com.laserxprts.falcon.exception.ResourceNotFoundException;
import com.laserxprts.falcon.model.Product;
import com.laserxprts.falcon.model.Wishlist;
import com.laserxprts.falcon.repository.ProductRepository;
import com.laserxprts.falcon.repository.WishlistRepository;

@ExtendWith(MockitoExtension.class)
public class WishlistServiceTest {

    @Mock private WishlistRepository wishlistRepository;
    @Mock private ProductRepository productRepository;

    @InjectMocks
    private WishlistService wishlistService;

    private Wishlist testWishlist;
    private Product testProduct;

    @BeforeEach
    void setUp() {
        testWishlist = new Wishlist();
        testWishlist.setId("WISH-1");
        testWishlist.setUserId("user-1");
        testWishlist.setProducts(new ArrayList<>());

        testProduct = new Product();
        testProduct.setId("PROD-1");
    }

    @Test
    void testToggleWishlist_AddProduct() {
        when(wishlistRepository.findByUserId("user-1")).thenReturn(Optional.of(testWishlist));
        when(productRepository.findById("PROD-1")).thenReturn(Optional.of(testProduct));
        when(wishlistRepository.save(any(Wishlist.class))).thenReturn(testWishlist);

        WishlistResponse response = wishlistService.toggleWishlist("user-1", "PROD-1");

        assertNotNull(response);
        assertEquals(1, testWishlist.getProducts().size());
        assertEquals("PROD-1", testWishlist.getProducts().get(0).getId());
        verify(wishlistRepository).save(any(Wishlist.class));
    }

    @Test
    void testToggleWishlist_RemoveProduct() {
        testWishlist.getProducts().add(testProduct); // Add initially

        when(wishlistRepository.findByUserId("user-1")).thenReturn(Optional.of(testWishlist));
        when(productRepository.findById("PROD-1")).thenReturn(Optional.of(testProduct));
        when(wishlistRepository.save(any(Wishlist.class))).thenReturn(testWishlist);

        WishlistResponse response = wishlistService.toggleWishlist("user-1", "PROD-1");

        assertNotNull(response);
        assertTrue(testWishlist.getProducts().isEmpty()); // Should be removed
        verify(wishlistRepository).save(any(Wishlist.class));
    }

    @Test
    void testToggleWishlist_ProductNotFound_ThrowsException() {
        when(wishlistRepository.findByUserId("user-1")).thenReturn(Optional.of(testWishlist));
        when(productRepository.findById("PROD-1")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> wishlistService.toggleWishlist("user-1", "PROD-1"));
        verify(wishlistRepository, never()).save(any(Wishlist.class));
    }
}
