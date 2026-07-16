package com.laserxprts.falcon.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.laserxprts.falcon.model.WhatsappSettings;

@Repository
public interface WhatsappSettingsRepository extends MongoRepository<WhatsappSettings, String> {
}
