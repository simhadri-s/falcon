package com.laserxprts.falcon.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Document(collection = "addresses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Address {
    
    @Id 
    private String id;

    private String fullName;
    private String phoneNumber;

    private String street;
    private String city; 
    private String state;
    private String pincode;
    private String country;

    private boolean isDefault;

    @Indexed
    private String userId;
}
