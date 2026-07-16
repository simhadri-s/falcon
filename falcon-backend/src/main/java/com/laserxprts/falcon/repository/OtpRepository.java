package com.laserxprts.falcon.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.laserxprts.falcon.model.Otp;

@Repository
public interface OtpRepository extends MongoRepository< Otp, String> {
    Optional<Otp> findByEmail(String email);

    @Transactional
    void deleteByEmail(String email);
}
