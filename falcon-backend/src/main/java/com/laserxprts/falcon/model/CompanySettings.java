package com.laserxprts.falcon.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

@Data
@Document(collection = "company_settings")
public class CompanySettings {
    
        @Id
    private String id;              // e.g. "COMPANY_SETTINGS"
    private String companyName;     // e.g. "LaserXprts Falcon"
    private String email;           // e.g. "support@falcon.com"
    private String phone;           // e.g. "+91-9876543210"
    private String address;         // e.g. "Hosur, Tamil Nadu"
    private String logoUrl;         // e.g. "https://res.cloudinary.com/..."
    private String workingHours;    // e.g. "9 AM - 6 PM"
    private Integer returnWindowDays; // e.g. 7
    private String termsAndConditions;
    private String privacyPolicy;
    private String orderIdPrefix;   // e.g. "FAL"

}


