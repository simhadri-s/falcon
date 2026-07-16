package com.laserxprts.falcon.repository;

import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import java.util.List;
import com.laserxprts.falcon.model.ProductCategory;

@Repository
public interface ProductCategoryRepository extends MongoRepository<ProductCategory, String> {
    @Cacheable("categories")
    List<ProductCategory> findAll();

    @CacheEvict(value = "categories", allEntries = true)
    <S extends ProductCategory> S save(S entity);

    @CacheEvict(value = "categories", allEntries = true)
    <S extends ProductCategory> List<S> saveAll(Iterable<S> entities);

    @CacheEvict(value = "categories", allEntries = true)
    void deleteById(String id);

    @Cacheable("categories")
    Optional<ProductCategory> findByNameIgnoreCase(String name);
    
    @Cacheable("categories")
    Optional<ProductCategory> findBySlug(String slug);
    boolean existsByNameIgnoreCase(String name);
    boolean existsBySlug(String slug);
}