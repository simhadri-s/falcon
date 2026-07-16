package com.laserxprts.falcon.repository;

import java.util.Optional;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import com.laserxprts.falcon.model.Product;

@Repository
public interface ProductRepository extends MongoRepository<Product, String>, ProductRepositoryCustom {
    
    // --------------------------------------------------------
    // ADMIN METHODS (Fetches everything: Drafts + Published)
    // --------------------------------------------------------
    Optional<Product> findBySlug(String slug);
    
    Page<Product> findByCategoryIgnoreCase(String category, Pageable pageable);
    Page<Product> findByCategoryId(String categoryId, Pageable pageable);
    Page<Product> findBySubCategoryId(String subCategoryId, Pageable pageable);
    List<Product> findByCategoryId(String categoryId);
    List<Product> findBySubCategoryId(String subCategoryId);
    
    @Query("{ 'industries.slug': ?0 }")
    Page<Product> findByIndustriesSlug(String slug, Pageable pageable);

    boolean existsBySlug(String slug);
    
    Page<Product> findByIsFeaturedTrue(Pageable pageable);

    // --------------------------------------------------------
    // PUBLIC METHODS (Fetches ONLY Published items)
    // --------------------------------------------------------
    
    // For fetching the public product catalog
    Page<Product> findByPublishedTrue(Pageable pageable);
    
    List<Product> findByPublishedTrue();
    
    Page<Product> findByCategoryIgnoreCaseAndPublishedTrue(String category, Pageable pageable);
    Page<Product> findByCategoryIdAndPublishedTrue(String categoryId, Pageable pageable);
    Page<Product> findBySubCategoryIdAndPublishedTrue(String subCategoryId, Pageable pageable);
    
    Optional<Product> findBySlugAndPublishedTrue(String slug);

    Page<Product> findByIsFeaturedTrueAndPublishedTrue(Pageable pageable);

    List<Product> findByExpiryOfferTrueAndPublishedTrue();
    
    List<Product> findByAutoOfferOnExpiryTrueOrExpiryOfferTrue();

    @Query("{ 'published': true, 'expiryOffer': false, 'sellingPrice': { $exists: true, $ne: null }, 'mrp': { $exists: true, $ne: null }, '$expr': { '$lt': [ '$sellingPrice', '$mrp' ] } }")
    List<Product> findManualOffers();

    boolean existsByProductCode(String productCode);
}