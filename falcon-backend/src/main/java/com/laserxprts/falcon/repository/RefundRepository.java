package com.laserxprts.falcon.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.laserxprts.falcon.model.Refund;

@Repository
public interface RefundRepository extends MongoRepository<Refund, String> {
    List<Refund> findByUserId(String userId);
    List<Refund> findByOrderId(String orderId);
    List<Refund> findByReturnRequestId(String returnRequestId);
}
