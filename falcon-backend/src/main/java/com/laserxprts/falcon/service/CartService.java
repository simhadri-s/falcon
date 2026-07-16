package com.laserxprts.falcon.service;

import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.laserxprts.falcon.dto.request.AddToCartRequest;
import com.laserxprts.falcon.dto.response.CartResponse;
import com.laserxprts.falcon.exception.ApiException;
import com.laserxprts.falcon.exception.ResourceNotFoundException;
import com.laserxprts.falcon.model.Cart;
import com.laserxprts.falcon.model.CartItem;
import com.laserxprts.falcon.model.Product;
import com.laserxprts.falcon.model.Product.ProductVariant;
import com.laserxprts.falcon.repository.CartRepository;
import com.laserxprts.falcon.repository.ProductRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final ProductRepository productRepository;

    private CartResponse buildCartResponse(Cart cart) {
        if (cart == null)
            return null;

        java.util.List<String> productIds = cart.getItems().stream()
                .filter(item -> item.getProduct() != null)
                .map(item -> item.getProduct().getId())
                .collect(java.util.stream.Collectors.toList());

        java.util.Map<String, Product> productMap = productRepository.findAllById(productIds).stream()
                .collect(java.util.stream.Collectors.toMap(Product::getId, p -> p));

        CartResponse response = CartResponse.from(cart, productMap);
        if (response == null)
            return null;

        double subtotal = 0;
        for (CartItem item : cart.getItems()) {
            if (!item.isActive() || item.getProduct() == null || !productMap.containsKey(item.getProduct().getId()))
                continue;
            Product product = productMap.get(item.getProduct().getId());
            
            double price = 0.0;
            if (product.isHasVariants() && item.getVariantId() != null) {
                ProductVariant variant = product.getVariants().stream()
                    .filter(v -> v.getId().equals(item.getVariantId()))
                    .findFirst().orElse(null);
                if (variant != null) {
                    price = variant.getSellingPrice() != null && variant.getSellingPrice() > 0
                        ? variant.getSellingPrice()
                        : (variant.getMrp() != null ? variant.getMrp() : 0.0);
                }
            } else {
                price = product.getSellingPrice() != null && product.getSellingPrice() > 0
                        ? product.getSellingPrice()
                        : (product.getMrp() != null ? product.getMrp() : 0.0);
            }
            subtotal += price * item.getQuantity();
        }

        return response.toBuilder()
                .subtotal(subtotal)
                .totalAmount(subtotal) // totalDiscount will be 0 for now as auto-offers are gone
                .build();
    }

    public CartResponse addToCart(String userId, AddToCartRequest request) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseGet(() -> {
                    Cart newCart = new Cart();
                    newCart.setUserId(userId);
                    return newCart;
                });

        Optional<CartItem> existingItem = cart.getItems().stream()
                .filter(item -> item.getProduct().getId().equals(request.getProductId()) && 
                                (request.getVariantId() == null ? item.getVariantId() == null : request.getVariantId().equals(item.getVariantId())))
                .findFirst();

        if (existingItem.isPresent()) {
            existingItem.get().setQuantity(existingItem.get().getQuantity() + request.getQuantity());
            existingItem.get().setActive(true);
        } else {
            Product product = productRepository.findById(request.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

            if (product.isHasVariants()) {
                if (request.getVariantId() == null || request.getVariantId().isBlank()) {
                    throw new ApiException(HttpStatus.BAD_REQUEST, "Please select a product variant.");
                }
                boolean variantExists = product.getVariants().stream().anyMatch(v -> v.getId().equals(request.getVariantId()));
                if (!variantExists) {
                    throw new ResourceNotFoundException("Selected variant not found");
                }
            } else if (request.getVariantId() != null && !request.getVariantId().isBlank()) {
                 throw new ApiException(HttpStatus.BAD_REQUEST, "This product does not have variants.");
            }

            CartItem newItem = new CartItem();
            newItem.setProduct(product);
            newItem.setVariantId(request.getVariantId());
            newItem.setQuantity(request.getQuantity());

            cart.getItems().add(newItem);
        }

        Cart savedCart = cartRepository.save(cart);

        return buildCartResponse(savedCart);
    }

    public CartResponse getCartByUserId(String userId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseGet(() -> {
                    Cart newCart = new Cart();
                    newCart.setUserId(userId);
                    return newCart;
                });
        return buildCartResponse(cart);
    }

    public CartResponse removeFromCart(String userId, String productId, String variantId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found"));
        boolean removed = cart.getItems().removeIf(item -> 
            item.getProduct().getId().equals(productId) &&
            (variantId == null ? item.getVariantId() == null : variantId.equals(item.getVariantId()))
        );

        if (!removed) {
            throw new ResourceNotFoundException("Item not found in cart");
        }
        return buildCartResponse(cartRepository.save(cart));
    }

    public CartResponse updateQuantity(String userId, String productId, String variantId, int quantity) {
        if (productId == null || productId.isBlank()) {
            throw new IllegalArgumentException("Product ID cannot be null or empty");
        }
        if (quantity <= 0) {
            return removeFromCart(userId, productId, variantId);
        }
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found"));

        CartItem existingItem = cart.getItems().stream()
                .filter(item -> item.getProduct().getId().equals(productId) && 
                                (variantId == null ? item.getVariantId() == null : variantId.equals(item.getVariantId())))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Item not found in cart"));

        existingItem.setQuantity(quantity);
        Cart savedCart = cartRepository.save(cart);

        return buildCartResponse(savedCart);
    }

    public CartResponse clearCart(String userId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found"));

        cart.getItems().clear();
        Cart savedCart = cartRepository.save(cart);
        return buildCartResponse(savedCart);
    }

    public CartResponse updateActiveStatus(String userId, String productId, String variantId, boolean active) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found"));

        CartItem existingItem = cart.getItems().stream()
                .filter(item -> item.getProduct().getId().equals(productId) && 
                                (variantId == null ? item.getVariantId() == null : variantId.equals(item.getVariantId())))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Item not found in cart"));

        existingItem.setActive(active);
        return CartResponse.from(cartRepository.save(cart));
    }

    public CartResponse updateAllActiveStatus(String userId, boolean active) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found"));

        cart.getItems().forEach(item -> item.setActive(active));
        return CartResponse.from(cartRepository.save(cart));
    }
}
