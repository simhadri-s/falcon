package com.laserxprts.falcon.repository;

import com.laserxprts.falcon.model.CompanyImage;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import java.util.List;

@Repository
public interface CompanyImageRepository extends MongoRepository<CompanyImage, String> {
    @Cacheable("companyImages")
    List<CompanyImage> findAll();

    @CacheEvict(value = "companyImages", allEntries = true)
    <S extends CompanyImage> S save(S entity);

    @CacheEvict(value = "companyImages", allEntries = true)
    <S extends CompanyImage> List<S> saveAll(Iterable<S> entities);

    @CacheEvict(value = "companyImages", allEntries = true)
    void deleteById(String id);
}
