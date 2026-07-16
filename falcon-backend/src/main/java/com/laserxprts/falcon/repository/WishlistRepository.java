package com.laserxprts.falcon.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.laserxprts.falcon.model.Wishlist;

@Repository
public interface WishlistRepository extends MongoRepository<Wishlist, String> {
    Optional<Wishlist> findByUserId(String userId);
    void deleteByUserId(String userId);
    java.util.List<Wishlist> findByProductsId(String productId);
}
