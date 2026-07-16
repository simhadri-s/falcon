package com.laserxprts.falcon.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.laserxprts.falcon.model.Job;

@Repository
public interface JobRepository extends MongoRepository< Job, String> {
    
    Page<Job> getByActiveTrue(Pageable pageable);
}
