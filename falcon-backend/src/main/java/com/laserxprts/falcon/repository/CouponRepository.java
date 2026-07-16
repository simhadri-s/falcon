package com.laserxprts.falcon.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import com.laserxprts.falcon.model.Coupon;

public interface CouponRepository extends MongoRepository<Coupon, String> {

    Optional<Coupon> findByCodeIgnoreCase(String code);

    @Query("{ 'code': { $regex: ?0, $options: 'i' } }")
    Page<Coupon> searchByCode(String keyword, Pageable pageable);
}
