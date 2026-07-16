package com.laserxprts.falcon.repository;


import org.springframework.data.mongodb.repository.MongoRepository;
import com.laserxprts.falcon.model.CompanySettings;

public interface CompanySettingsRepository extends MongoRepository<CompanySettings, String> {
}

