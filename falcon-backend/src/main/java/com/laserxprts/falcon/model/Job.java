package com.laserxprts.falcon.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
@Getter
@Setter
@Document(collection = "jobs")
public class Job {
    @Id
    private String id;


    @NotBlank
    @Size(max = 150, message = "Title cannot exceed 150 characters")
    private String title;

    @NotBlank
    private String department;

    @NotBlank
    private String location;

    @NotBlank
    private String  type;

    private boolean  active;

}

