package com.laserxprts.falcon.dto.response;

import java.time.LocalDateTime;

import com.laserxprts.falcon.model.Inquiry;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class InquiryResponse {
    private String id;
    private String name;
    private String email;
    private String phone;
    private String subject;
    private String message;
    private String status;
    private LocalDateTime createdAt;

    public static InquiryResponse from(Inquiry inquiry) {
        if (inquiry == null) {
            return null;
        }
        
        return InquiryResponse.builder()
                .id(inquiry.getId())
                .name(inquiry.getName())
                .email(inquiry.getEmail())
                .phone(inquiry.getPhone())
                .subject(inquiry.getSubject())
                .message(inquiry.getMessage())
                .status(inquiry.getStatus())
                .createdAt(inquiry.getCreatedAt())
                .build();
    }
}