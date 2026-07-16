package com.laserxprts.falcon.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.laserxprts.falcon.model.ReturnRequest;

@Repository
public interface ReturnRequestRepository extends MongoRepository<ReturnRequest, String> {
    List<ReturnRequest> findByUserId(String userId);
    List<ReturnRequest> findByOrderId(String orderId);
    List<ReturnRequest> findByOrderIdIn(Iterable<String> orderIds);
    Optional<ReturnRequest> findByOrderIdAndStatusNot(String orderId, com.laserxprts.falcon.enums.ReturnStatus status);
}
