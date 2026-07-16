package com.laserxprts.falcon.model;

import org.hibernate.validator.constraints.URL;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Document(collection = "industries")
public class Industry {
    
    @Id
    private String id;

    @NotBlank
    @Size(max = 100)
    private String name;

    @NotBlank
    @Indexed(unique = true)
    @Pattern(regexp = "^[a-z0-9-]+$", message = "Slug must be URL-safe")
    private String slug;

    @URL(message = "Icon must be a valid URL")
    private String iconUrl;

    @Size(max = 1000, message = "Description cannot exceed 1000 characters")
    private String description;
}
