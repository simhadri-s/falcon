package com.laserxprts.falcon.repository;

import com.laserxprts.falcon.model.Order;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderRepository extends MongoRepository<Order, String> {

    @org.springframework.data.mongodb.repository.Aggregation(pipeline = {
        "{ $match: { userId: { $ne: null } } }",
        "{ $group: { _id: '$userId' } }"
    })
    java.util.List<String> findDistinctUserIds();

    java.util.List<Order> findByCreatedAtGreaterThanEqualOrderByCreatedAtDesc(java.time.LocalDateTime createdAt);
    
    java.util.List<Order> findAllByOrderByCreatedAtDesc();

    // --------------------------------------------------------
    // ADMIN METHODS (Searches across ALL orders)
    // --------------------------------------------------------
    Page<Order> findByStatusIgnoreCase(String status, Pageable pageable);

    @Query("{ $or: [ { '_id': { $regex: ?0, $options: 'i' } }, { 'userId': { $regex: ?0, $options: 'i' } }, { 'addressSnapshot.fullName': { $regex: ?0, $options: 'i' } }, { 'addressSnapshot.phoneNumber': { $regex: ?0, $options: 'i' } } ] }")
    Page<Order> searchByKeyword(String keyword, Pageable pageable);

    @Query("{ 'status': { $regex: ?1, $options: 'i' }, $or: [ { '_id': { $regex: ?0, $options: 'i' } }, { 'userId': { $regex: ?0, $options: 'i' } }, { 'addressSnapshot.fullName': { $regex: ?0, $options: 'i' } }, { 'addressSnapshot.phoneNumber': { $regex: ?0, $options: 'i' } } ] }")
    Page<Order> searchByKeywordAndStatus(String keyword, String status, Pageable pageable);

    long countByStatus(String status);

    // --------------------------------------------------------
    // USER METHODS (Strictly filters by their specific userId)
    // --------------------------------------------------------
    Page<Order> findByUserId(String userId, Pageable pageable);
    
    Page<Order> findByUserIdAndStatusIgnoreCase(String userId, String status, Pageable pageable);

    @Query("{ 'userId': ?0, $or: [ { '_id': { $regex: ?1, $options: 'i' } }, { 'addressSnapshot.fullName': { $regex: ?1, $options: 'i' } }, { 'addressSnapshot.phoneNumber': { $regex: ?1, $options: 'i' } } ] }")
    Page<Order> searchByUserIdAndKeyword(String userId, String keyword, Pageable pageable);

    @Query("{ 'userId': ?0, 'status': { $regex: ?2, $options: 'i' }, $or: [ { '_id': { $regex: ?1, $options: 'i' } }, { 'addressSnapshot.fullName': { $regex: ?1, $options: 'i' } }, { 'addressSnapshot.phoneNumber': { $regex: ?1, $options: 'i' } } ] }")
    Page<Order> searchByUserIdAndKeywordAndStatus(String userId, String keyword, String status, Pageable pageable);

    Optional<Order> findByUserIdAndId(String userId, String id);

    boolean existsByUserIdAndStatusIgnoreCaseAndItemsProductSnapshotId(String userId, String status, String productId);
}
