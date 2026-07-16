package com.laserxprts.falcon.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import com.laserxprts.falcon.model.SubCategory;

@Repository
public interface SubCategoryRepository extends MongoRepository<SubCategory, String> {
    @Cacheable("subcategories")
    List<SubCategory> findAll();

    @CacheEvict(value = "subcategories", allEntries = true)
    <S extends SubCategory> S save(S entity);

    @CacheEvict(value = "subcategories", allEntries = true)
    <S extends SubCategory> List<S> saveAll(Iterable<S> entities);

    @CacheEvict(value = "subcategories", allEntries = true)
    void deleteById(String id);

    @Cacheable("subcategories")
    List<SubCategory> findByCategoryId(String categoryId);

    @Cacheable("subcategories")
    Optional<SubCategory> findByNameIgnoreCase(String name);

    @Cacheable("subcategories")
    Optional<SubCategory> findBySlug(String slug);
    boolean existsByNameIgnoreCase(String name);
    boolean existsBySlug(String slug);
    @CacheEvict(value = "subcategories", allEntries = true)
    void deleteByCategoryId(String categoryId);
}
