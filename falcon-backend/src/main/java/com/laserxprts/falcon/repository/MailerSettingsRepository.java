package com.laserxprts.falcon.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import com.laserxprts.falcon.model.MailerSettings;

public interface MailerSettingsRepository extends MongoRepository<MailerSettings, String> {
}
