package com.laserxprts.falcon.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Document(collection = "sub_categories")
public class SubCategory {
    @Id
    private String id;

    @NotBlank
    @Indexed(unique = true)
    private String name;

    @Indexed(unique = true)
    private String slug;

    @NotBlank
    @Indexed
    private String categoryId;
}
