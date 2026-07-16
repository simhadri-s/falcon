package com.laserxprts.falcon.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;

import com.laserxprts.falcon.model.Role;
import com.laserxprts.falcon.model.User;

@Repository
public interface UserRepository extends MongoRepository<User, String> {
    @Cacheable(value = "users", key = "#email")
    Optional<User> findByEmail(String email);

    @CacheEvict(value = "users", key = "#entity.email")
    <S extends User> S save(S entity);

    @CacheEvict(value = "users", allEntries = true)
    <S extends User> List<S> saveAll(Iterable<S> entities);

    @CacheEvict(value = "users", allEntries = true)
    void deleteById(String id);
    
    List<User> findByEmailIn(java.util.Collection<String> emails);

    boolean existsByEmail(String email);

    boolean existsByRoles(Role role);

    List<User> findByRoles(Role role);

    @Query("""
        {
        'roles': ?0,
        $or: [
            { 'name': { $regex: ?1, $options: 'i' } },
            { 'email': { $regex: ?1, $options: 'i' } }
        ]
        }
    """)
    List<User> findByRolesAndKeyword(Role role, String keyword);

    @Query("{ 'fcmTokens.0': { $exists: true } }")
    List<User> findUsersWithFcmTokens();

    @Query("{ 'fcmTokens': { $in: ?0 } }")
    List<User> findUsersByFcmTokensIn(List<String> tokens);
}