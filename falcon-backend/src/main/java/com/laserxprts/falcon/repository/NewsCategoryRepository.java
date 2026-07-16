package com.laserxprts.falcon.repository;

import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import java.util.List;
import com.laserxprts.falcon.model.NewsCategory;

@Repository
public interface NewsCategoryRepository extends MongoRepository<NewsCategory, String> {
    @Cacheable("newsCategories")
    List<NewsCategory> findAll();

    @CacheEvict(value = "newsCategories", allEntries = true)
    <S extends NewsCategory> S save(S entity);

    @CacheEvict(value = "newsCategories", allEntries = true)
    <S extends NewsCategory> List<S> saveAll(Iterable<S> entities);

    @CacheEvict(value = "newsCategories", allEntries = true)
    void deleteById(String id);

    @Cacheable("newsCategories")
    Optional<NewsCategory> findByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCase(String name);
    boolean existsBySlug(String slug);
}