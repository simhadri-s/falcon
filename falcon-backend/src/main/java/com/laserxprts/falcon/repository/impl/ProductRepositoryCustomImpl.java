package com.laserxprts.falcon.repository.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import com.laserxprts.falcon.model.Product;
import com.laserxprts.falcon.repository.ProductRepositoryCustom;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class ProductRepositoryCustomImpl implements ProductRepositoryCustom {

    private final MongoTemplate mongoTemplate;

    @Override
    public Page<Product> searchByKeyword(
            @NonNull String keyword,
            @NonNull Pageable pageable,
            boolean isAdmin
    ) {

        Objects.requireNonNull(keyword);
        Objects.requireNonNull(pageable);

        String escapedKeyword = Pattern.quote(keyword);

        List<Criteria> searchCriteria = new ArrayList<>();

        // Partial search across multiple fields
        searchCriteria.add(Criteria.where("name").regex(escapedKeyword, "i"));
        searchCriteria.add(Criteria.where("description").regex(escapedKeyword, "i"));
        searchCriteria.add(Criteria.where("category").regex(escapedKeyword, "i"));
        searchCriteria.add(Criteria.where("productCode").regex(escapedKeyword, "i"));
        searchCriteria.add(Criteria.where("slug").regex(escapedKeyword, "i"));

        Criteria finalCriteria = new Criteria().orOperator(
                searchCriteria.toArray(new Criteria[0])
        );

        // Public users should see only published products
        if (!isAdmin) {
            finalCriteria = new Criteria().andOperator(
                    finalCriteria,
                    Criteria.where("published").is(true)
            );
        }

        Query query = new Query(finalCriteria);
        query.with(pageable);

        List<Product> products = mongoTemplate.find(query, Product.class);

        long count = mongoTemplate.count(
                Query.of(query).limit(-1).skip(-1),
                Product.class
        );

        return new PageImpl<>(products, pageable, count);
    }

    @Override
    public Page<Product> filterByCategoryAndKeyword(
            String category,
            String keyword,
            Pageable pageable,
            boolean isAdmin
    ) {

        Objects.requireNonNull(keyword);
        Objects.requireNonNull(category);
        Objects.requireNonNull(pageable);

        String escapedKeyword = Pattern.quote(keyword);

        // Use categoryId with exact match (resolved in service)
        Criteria categoryCriteria =
                Criteria.where("categoryId").is(category);

        Criteria keywordCriteria = new Criteria().orOperator(
                Criteria.where("name").regex(escapedKeyword, "i"),
                Criteria.where("description").regex(escapedKeyword, "i"),
                Criteria.where("productCode").regex(escapedKeyword, "i"),
                Criteria.where("slug").regex(escapedKeyword, "i")
        );

        Criteria finalCriteria = new Criteria().andOperator(
                categoryCriteria,
                keywordCriteria
        );

        // Public users should see only published products
        if (!isAdmin) {
            finalCriteria = new Criteria().andOperator(
                    finalCriteria,
                    Criteria.where("published").is(true)
            );
        }

        Query query = new Query(finalCriteria);
        query.with(pageable);

        List<Product> products = mongoTemplate.find(query, Product.class);

        long count = mongoTemplate.count(
                Query.of(query).limit(-1).skip(-1),
                Product.class
        );

        return new PageImpl<>(products, pageable, count);
    }

    @Override
    public Page<Product> filterBySubCategoryAndKeyword(
            String subCategory,
            String keyword,
            Pageable pageable,
            boolean isAdmin
    ) {

        Objects.requireNonNull(keyword);
        Objects.requireNonNull(subCategory);
        Objects.requireNonNull(pageable);

        String escapedKeyword = Pattern.quote(keyword);

        Criteria subCategoryCriteria =
                Criteria.where("subCategoryId").is(subCategory);

        Criteria keywordCriteria = new Criteria().orOperator(
                Criteria.where("name").regex(escapedKeyword, "i"),
                Criteria.where("description").regex(escapedKeyword, "i"),
                Criteria.where("productCode").regex(escapedKeyword, "i"),
                Criteria.where("slug").regex(escapedKeyword, "i")
        );

        Criteria finalCriteria = new Criteria().andOperator(
                subCategoryCriteria,
                keywordCriteria
        );

        // Public users should see only published products
        if (!isAdmin) {
            finalCriteria = new Criteria().andOperator(
                    finalCriteria,
                    Criteria.where("published").is(true)
            );
        }

        Query query = new Query(finalCriteria);
        query.with(pageable);

        List<Product> products = mongoTemplate.find(query, Product.class);

        long count = mongoTemplate.count(
                Query.of(query).limit(-1).skip(-1),
                Product.class
        );

        return new PageImpl<>(products, pageable, count);
    }
}