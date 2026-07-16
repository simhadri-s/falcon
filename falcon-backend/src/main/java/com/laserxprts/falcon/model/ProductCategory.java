package com.laserxprts.falcon.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Document(collection = "product_categories")
public class ProductCategory {
    @Id
    private String id;

    @NotBlank
    @Indexed(unique = true)
    private String name;

    @Indexed(unique = true)
    private String slug;

    private String imageUrl;
}