package com.laserxprts.falcon.dto.request;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NewsArticleRequest {

    @NotBlank
    private String title;

    @NotBlank
    private String content;

    private String category;
    private List<MultipartFile> images;
}
