package com.laserxprts.falcon.model;




import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Document(collection = "company_images")
public class CompanyImage {

    @Id
    private String id;
    private String name;
    private String description;
    private String logoUrl;   // store image URL or path
    private String iconUrl;
    private String faviconUrl;
    private String landingPageImageUrl;
    @CreatedDate
    private String createdAt;
}
