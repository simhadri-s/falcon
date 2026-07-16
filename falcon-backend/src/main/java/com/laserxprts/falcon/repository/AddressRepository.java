package com.laserxprts.falcon.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.laserxprts.falcon.model.Address;

@Repository
public interface AddressRepository extends MongoRepository<Address, String> {

    List<Address> findByUserId(String userId);
    void deleteByUserId(String userId);

    Optional<Address> findByIdAndUserId(String id, String userId);
    Optional<Address> findByUserIdAndIsDefaultTrue(String userId);

    @org.springframework.data.mongodb.repository.Aggregation("{ '$group': { '_id': '$phoneNumber' } }")
    List<String> findDistinctPhoneNumbers();
}
    

