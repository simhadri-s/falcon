package com.laserxprts.falcon.dto.response;

import com.laserxprts.falcon.model.Job;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class JobResponse {
    private String id;
    private String title;
    private String department;
    private String location;
    private String type;
    private boolean isActive;

    public static JobResponse from(Job job) {
        if (job == null) {
            return null;
        }
        
        return JobResponse.builder()
                .id(job.getId())
                .title(job.getTitle())
                .department(job.getDepartment())
                .location(job.getLocation())
                .type(job.getType())
                .isActive(job.isActive())
                .build();
    }
}