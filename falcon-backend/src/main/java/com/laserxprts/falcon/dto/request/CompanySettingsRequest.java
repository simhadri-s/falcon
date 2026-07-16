package com.laserxprts.falcon.dto.request;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CompanySettingsRequest {

    private String companyName;
    private String email;
    private String phone;
    private String address;
    private String workingHours;
}
