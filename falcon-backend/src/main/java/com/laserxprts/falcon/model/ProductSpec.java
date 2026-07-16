package com.laserxprts.falcon.model;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductSpec {
    @Size(max = 100, message = "Key cannot exceed 100 characters")
    private String key;

    @Size(max = 500, message = "Value cannot exceed 500 characters")
    private String value;
}
