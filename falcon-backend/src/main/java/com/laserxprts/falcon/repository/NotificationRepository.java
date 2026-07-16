package com.laserxprts.falcon.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import com.laserxprts.falcon.model.Notification;

@Repository
public interface NotificationRepository extends MongoRepository<Notification, String> {
    
    // Find notifications that belong to the specific user OR are broadcasts (userId is null)
    @Query("{ $or: [ { 'userId': ?0 }, { 'userId': null } ] }")
    Page<Notification> findByUserIdOrBroadcast(String userId, Pageable pageable);
    
    long countByUserIdAndReadFalse(String userId);

    void deleteByUserId(String userId);
}
