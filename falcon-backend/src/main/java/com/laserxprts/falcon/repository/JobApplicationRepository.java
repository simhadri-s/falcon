package com.laserxprts.falcon.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.laserxprts.falcon.model.JobApplication;
import java.util.List;

@Repository
public interface JobApplicationRepository extends MongoRepository<JobApplication, String> {
    void deleteByJobId(String jobId);
    List<JobApplication> findByJobId(String jobId);
}
