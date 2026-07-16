package com.laserxprts.falcon.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;

import com.laserxprts.falcon.model.Banner;

public interface BannerRepository extends MongoRepository<Banner,String>{

    @Cacheable("banners")
    List<Banner> findAll();

    @CacheEvict(value = "banners", allEntries = true)
    <S extends Banner> S save(S entity);

    @CacheEvict(value = "banners", allEntries = true)
    <S extends Banner> List<S> saveAll(Iterable<S> entities);

    @CacheEvict(value = "banners", allEntries = true)
    void deleteById(String id);

    @Cacheable("banners")
    List<Banner> findByActiveTrue();

    @Cacheable("banners")
    Banner findByDefaultBannerTrue();
    
}
