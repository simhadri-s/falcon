package com.laserxprts.falcon.service;

import java.util.ArrayList;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.laserxprts.falcon.dto.request.AddToCartRequest;
import com.laserxprts.falcon.dto.response.CartResponse;
import com.laserxprts.falcon.exception.ResourceNotFoundException;
import com.laserxprts.falcon.model.Cart;
import com.laserxprts.falcon.model.CartItem;
import com.laserxprts.falcon.model.Product;
import com.laserxprts.falcon.repository.CartRepository;
import com.laserxprts.falcon.repository.ProductRepository;

@ExtendWith(MockitoExtension.class)
public class CartServiceTest {

    @Mock private CartRepository cartRepository;
    @Mock private ProductRepository productRepository;

    @InjectMocks
    private CartService cartService;

    private Cart cart;
    private Product product;
    private AddToCartRequest request;

    @BeforeEach
    void setUp() {
        cart = new Cart();
        cart.setId("CART-1");
        cart.setUserId("user-1");
        cart.setItems(new ArrayList<>());

        product = new Product();
        product.setId("PROD-1");
        product.setSellingPrice(150.0);
        product.setMrp(200.0);

        request = new AddToCartRequest();
        request.setProductId("PROD-1");
        request.setQuantity(2);
    }

    @Test
    void testAddToCart_NewItem() {
        when(cartRepository.findByUserId("user-1")).thenReturn(Optional.of(cart));
        when(productRepository.findById("PROD-1")).thenReturn(Optional.of(product));
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);

        CartResponse response = cartService.addToCart("user-1", request);

        assertNotNull(response);
        assertEquals(1, cart.getItems().size());
        assertEquals(2, cart.getItems().get(0).getQuantity());
        assertEquals("PROD-1", cart.getItems().get(0).getProduct().getId());
        verify(cartRepository).save(any(Cart.class));
    }

    @Test
    void testAddToCart_ExistingItem_IncreasesQuantity() {
        CartItem existingItem = new CartItem();
        existingItem.setProduct(product);
        existingItem.setQuantity(1);
        cart.getItems().add(existingItem);

        when(cartRepository.findByUserId("user-1")).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);

        cartService.addToCart("user-1", request);

        assertEquals(1, cart.getItems().size());
        assertEquals(3, cart.getItems().get(0).getQuantity()); // 1 + 2
        verify(cartRepository).save(any(Cart.class));
    }

    @Test
    void testAddToCart_ProductNotFound_ThrowsException() {
        when(cartRepository.findByUserId("user-1")).thenReturn(Optional.of(cart));
        when(productRepository.findById("PROD-1")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> cartService.addToCart("user-1", request));
        verify(cartRepository, never()).save(any(Cart.class));
    }
}
