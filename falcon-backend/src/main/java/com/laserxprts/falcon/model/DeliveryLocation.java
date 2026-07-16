package com.laserxprts.falcon.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Document(collection = "delivery_locations")
public class DeliveryLocation {

    @Id
    private String id;

    @Indexed(unique = true)
    @Pattern(regexp = "^[1-9][0-9]{5}$", message = "Enter a proper pincode")
    private String pincode;
    private String location;
    private float deliveryCharge;
}
