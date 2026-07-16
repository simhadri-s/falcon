package com.laserxprts.falcon.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.NonNull;

import com.laserxprts.falcon.model.Product;

public interface ProductRepositoryCustom {
    Page<Product> searchByKeyword(@NonNull String keyword, @NonNull Pageable pageable, boolean isAdmin);
    Page<Product> filterByCategoryAndKeyword(String category, String keyword, Pageable pageable, boolean isAdmin);
    Page<Product> filterBySubCategoryAndKeyword(String subCategory, String keyword, Pageable pageable, boolean isAdmin);
}