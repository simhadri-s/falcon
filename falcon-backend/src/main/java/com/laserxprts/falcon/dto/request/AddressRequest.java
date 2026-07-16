 package com.laserxprts.falcon.dto.request;

import lombok.Data;

@Data
public class AddressRequest {

    private String fullName;
    private String phoneNumber;
    private String street;
    private String city;
    private String state;
    private String pincode;
    private String country;
    private boolean isDefault;
}
    

