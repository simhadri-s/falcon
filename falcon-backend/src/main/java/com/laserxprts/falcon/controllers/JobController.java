package com.laserxprts.falcon.controllers;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.laserxprts.falcon.dto.response.JobResponse;
import com.laserxprts.falcon.model.Job;
import com.laserxprts.falcon.security.PermissionService;
import com.laserxprts.falcon.service.JobService;

import jakarta.validation.Valid;

@PreAuthorize("@permissionService.hasAccess('MANAGE_JOBS')")
@RestController
@RequestMapping("/api/jobs")
public class JobController {
    
    private final JobService jobService;
    private final PermissionService permissionService;

    public JobController(JobService jobService, PermissionService permissionService) {
        this.jobService = jobService;
        this.permissionService = permissionService;
    }

    @PreAuthorize("permitAll()")
    @GetMapping
    public ResponseEntity<Map<String, Object>> getLatestJobs(
        @RequestParam(value = "page", defaultValue = "1") int page,
        @RequestParam(value = "limit", defaultValue = "10") int limit,
        @RequestParam(value = "sortBy", required = false) String sortBy,
        @RequestParam(value = "sortDirection", required = false) String sortDirection
    ) {

        boolean canViewInactive = permissionService.hasAccess("MANAGE_JOBS");
        Page<JobResponse> jobPage = jobService.getLatestJobs(page, limit, sortBy, sortDirection, canViewInactive);

        Map<String, Object> jobResponse = new HashMap<>();
        jobResponse.put("data", jobPage.getContent());
        jobResponse.put("total", jobPage.getTotalElements());
        jobResponse.put("page", page);
        jobResponse.put("pages", jobPage.getTotalPages());

        return ResponseEntity.ok(jobResponse);
    }

    @PostMapping
    public ResponseEntity<JobResponse> createJob(@Valid @RequestBody Job job) {
        Objects.requireNonNull(job);
        JobResponse createdJob = jobService.createJob(job);
        return new ResponseEntity<>(createdJob, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<JobResponse> updateJob(@PathVariable(value = "id") String id,@RequestBody Job updatedJob) {
        JobResponse responseJob = jobService.updateJob(id, updatedJob);
        return ResponseEntity.ok(responseJob);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteJob(@PathVariable(value = "id") String id) {
        jobService.deleteJob(id);
        return ResponseEntity.noContent().build();
    }
}
