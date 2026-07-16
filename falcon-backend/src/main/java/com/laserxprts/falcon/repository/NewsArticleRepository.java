package com.laserxprts.falcon.repository;

import java.util.Optional;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import com.laserxprts.falcon.model.NewsArticle;

@Repository
public interface NewsArticleRepository extends MongoRepository<NewsArticle, String> {
    
    // --------------------------------------------------------
    // ADMIN METHODS (Fetches everything: Drafts + Published)
    // --------------------------------------------------------
    Optional<NewsArticle> findBySlug(String slug);

    @Query("{ $or: [ { 'title': { $regex: ?0, $options: 'i' } }, { 'content': { $regex: ?0, $options: 'i' } } ] }")
    Page<NewsArticle> searchByKeyword(String keyword, Pageable pageable);

    @Query("{ $and: [ { $or: [ { 'title': { $regex: ?0, $options: 'i' } }, { 'content': { $regex: ?0, $options: 'i' } } ] }, { 'category': { $regex: ?1, $options: 'i' } } ] }")
    Page<NewsArticle> searchByKeywordAndCategory(String keyword, String category, Pageable pageable);  

    Page<NewsArticle> findByCategoryIgnoreCase(String category, Pageable pageable);
    List<NewsArticle> findByCategoryIgnoreCase(String category);

    boolean existsBySlug(String slug);


    // --------------------------------------------------------
    // PUBLIC METHODS (Fetches ONLY Published items)
    // --------------------------------------------------------
    
    // For the public news listing page
    Page<NewsArticle> findByPublishedTrue(Pageable pageable);

    // For public detail view
    Optional<NewsArticle> findBySlugAndPublishedTrue(String slug);

    // For public category filter
    Page<NewsArticle> findByCategoryIgnoreCaseAndPublishedTrue(String category, Pageable pageable);

    // NEW: Public keyword search (Requires 'published': true in the $and array)
    @Query("{ $and: [ { 'published': true }, { $or: [ { 'title': { $regex: ?0, $options: 'i' } }, { 'content': { $regex: ?0, $options: 'i' } } ] } ] }")
    Page<NewsArticle> searchByKeywordAndPublishedTrue(String keyword, Pageable pageable);

    // NEW: Public keyword + category search
    @Query("{ $and: [ { 'published': true }, { $or: [ { 'title': { $regex: ?0, $options: 'i' } }, { 'content': { $regex: ?0, $options: 'i' } } ] }, { 'category': { $regex: ?1, $options: 'i' } } ] }")
    Page<NewsArticle> searchByKeywordAndCategoryAndPublishedTrue(String keyword, String category, Pageable pageable);
}