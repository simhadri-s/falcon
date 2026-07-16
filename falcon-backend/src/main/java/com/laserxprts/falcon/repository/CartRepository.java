package com.laserxprts.falcon.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.laserxprts.falcon.model.Cart;

public interface CartRepository extends MongoRepository<Cart, String>{
    Optional<Cart> findByUserId(String userId);
    void deleteByUserId(String userId);
    java.util.List<Cart> findByItemsProductId(String productId);
}
