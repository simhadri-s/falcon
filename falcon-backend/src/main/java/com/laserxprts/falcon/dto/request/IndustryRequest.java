package com.laserxprts.falcon.dto.request;
import org.springframework.web.multipart.MultipartFile;


import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class IndustryRequest {


    @NotBlank
    private String name;
    private String description;
    private MultipartFile icon;
}
