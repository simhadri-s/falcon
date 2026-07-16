package com.laserxprts.falcon.dto.response;

import com.laserxprts.falcon.model.Industry;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class IndustryResponse {
    private String id;
    private String name;
    private String slug;
    private String iconUrl;
    private String description;

    public static IndustryResponse from(Industry industry) {
        if (industry == null) {
            return null;
        }
        
        return IndustryResponse.builder()
                .id(industry.getId())
                .name(industry.getName())
                .slug(industry.getSlug())
                .iconUrl(industry.getIconUrl())
                .description(industry.getDescription())
                .build();
    }
}