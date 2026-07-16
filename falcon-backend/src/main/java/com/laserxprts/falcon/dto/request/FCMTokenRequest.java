package com.laserxprts.falcon.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FCMTokenRequest {
    @NotBlank(message = "Token is required")
    private String token;
}
