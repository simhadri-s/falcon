package com.laserxprts.falcon.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.laserxprts.falcon.model.Inquiry;

@Repository
public interface InquiryRepository extends MongoRepository<Inquiry, String> {
    long countByStatus(String status);
}
