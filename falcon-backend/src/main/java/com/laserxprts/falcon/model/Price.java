package com.laserxprts.falcon.model;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Document(collection = "prices")
public class Price {
    @Id
    private String id;
    private double  price;
    private String currency;
    @CreatedDate
    private String createdAt;



}
