package com.laserxprts.falcon.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PriceRequest{
    private String id;
    private Double amount;
    private String currency;
    private String createdAt;
}
