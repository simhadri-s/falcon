package com.laserxprts.falcon.repository;
import com.laserxprts.falcon.model.Industry;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import java.util.List;

public interface IndustryRepository extends MongoRepository<Industry, String> {
    @Cacheable("industries")
    List<Industry> findAll();

    @CacheEvict(value = "industries", allEntries = true)
    <S extends Industry> S save(S entity);

    @CacheEvict(value = "industries", allEntries = true)
    <S extends Industry> List<S> saveAll(Iterable<S> entities);

    @CacheEvict(value = "industries", allEntries = true)
    void deleteById(String id);

    @Cacheable("industries")
    Optional<Industry> findBySlug(String slug);
    
    @Cacheable("industries")
    java.util.List<Industry> findBySlugIn(java.util.Collection<String> slugs);

    boolean existsBySlug(String slug);
}