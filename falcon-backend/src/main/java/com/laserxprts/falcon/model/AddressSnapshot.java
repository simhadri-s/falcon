package com.laserxprts.falcon.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class AddressSnapshot {
    private String id;
    private String fullName;
    private String phoneNumber;
    private String street;
    private String city;
    private String pincode;
    private String country;
    private String userId;


    public static AddressSnapshot from(Address address) {
        return AddressSnapshot.builder()
            .id(address.getId())
            .fullName(address.getFullName())
            .phoneNumber(address.getPhoneNumber())
            .street(address.getStreet())
            .city(address.getCity())
            .pincode(address.getPincode())
            .country(address.getCountry())
            .userId(address.getUserId())
            .build();
    }
}
