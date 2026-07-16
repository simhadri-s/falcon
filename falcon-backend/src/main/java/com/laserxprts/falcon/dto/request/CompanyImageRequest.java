package com.laserxprts.falcon.dto.request;

import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CompanyImageRequest {

    private MultipartFile logo;
    private MultipartFile icon;
    private MultipartFile favicon;
    private MultipartFile landingPageImage;

    @NotBlank(message = "Name cannot be blank")
    private String name;

    private String description;
}
