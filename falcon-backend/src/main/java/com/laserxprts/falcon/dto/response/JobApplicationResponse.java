package com.laserxprts.falcon.dto.response;

import com.laserxprts.falcon.model.JobApplication;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class JobApplicationResponse {
    private String id;
    private String name;
    private String email;
    private String phone;
    private String cvUrl;
    private String coverLetterUrl;
    private String jobId;

    public static JobApplicationResponse from(JobApplication application) {
        if (application == null) {
            return null;
        }
        
        return JobApplicationResponse.builder()
                .id(application.getId())
                .name(application.getName())
                .email(application.getEmail())
                .phone(application.getPhone())
                .cvUrl(application.getCvUrl())
                .coverLetterUrl(application.getCoverLetterUrl())
                .jobId(application.getJob() != null ? application.getJob().getId() : null)
                .build();
    }
}