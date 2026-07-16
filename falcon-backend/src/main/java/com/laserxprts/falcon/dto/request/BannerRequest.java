package com.laserxprts.falcon.dto.request;

import org.springframework.web.multipart.MultipartFile;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BannerRequest {

    private MultipartFile image;
    private String title;
    private String description;
    private boolean active;
}
