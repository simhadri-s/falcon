package com.laserxprts.falcon.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import com.laserxprts.falcon.model.DeliveryLocation;

@Document
public interface DeliveryLocationRepository extends MongoRepository<DeliveryLocation, String> {
    Optional<DeliveryLocation> findByPincode(String pincode);

    @Query("{ $or: [ " +
       "{ 'location': { $regex: ?0, $options: 'i' } }, " +
       "{ 'pincode': { $regex: ?0 } } " +
       "] }")
    Page<DeliveryLocation> search(String keyword, Pageable pageable);

    boolean existsByPincode(String pincode);
}
