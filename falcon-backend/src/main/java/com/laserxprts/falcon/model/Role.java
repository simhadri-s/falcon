package com.laserxprts.falcon.model;

import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Document(collection = "roles")
public class Role {
    
    @Id
    private String id;

    @NotBlank
    @Indexed(unique = true)
    private String name;

    private List<String> permissions;
}
