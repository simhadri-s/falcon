package com.laserxprts.falcon.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import java.util.List;

import com.laserxprts.falcon.model.Role;

public interface RoleRepository extends MongoRepository<Role, String> {
    @Cacheable("roles")
    List<Role> findAll();

    @CacheEvict(value = "roles", allEntries = true)
    <S extends Role> S save(S entity);

    @CacheEvict(value = "roles", allEntries = true)
    <S extends Role> List<S> saveAll(Iterable<S> entities);

    @CacheEvict(value = "roles", allEntries = true)
    void deleteById(String id);

    @Cacheable("roles")
    Optional<Role> findByName(String role);
}
