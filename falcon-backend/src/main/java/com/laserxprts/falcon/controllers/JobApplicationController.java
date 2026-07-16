package com.laserxprts.falcon.controllers;

import java.util.HashMap;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.laserxprts.falcon.dto.request.JobApplicationRequest;
import com.laserxprts.falcon.dto.response.JobApplicationResponse;
import com.laserxprts.falcon.service.JobApplicationService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/applications")
public class JobApplicationController {

    private final JobApplicationService jobApplicationService;

    public JobApplicationController(JobApplicationService jobApplicationService) {
        this.jobApplicationService = jobApplicationService;
    }
    
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<JobApplicationResponse> createJobApplication(@Valid @ModelAttribute JobApplicationRequest application) {
        JobApplicationResponse savedJobAppllication = jobApplicationService.createJobApplication(application);
        return ResponseEntity.ok(savedJobAppllication);
    }

    @PreAuthorize("@permissionService.hasAccess('MANAGE_JOBAPPLICATIONS')")
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllApplications(
        @RequestParam(value = "page", defaultValue = "1") int page,
        @RequestParam(value = "limit", defaultValue = "10") int limit,
        @RequestParam(value = "sortBy", required = false) String sortBy,
        @RequestParam(value = "sortDirection", required = false) String sortDirection
    ) {
        Page<JobApplicationResponse> result = jobApplicationService.getAllApplications(page, limit, sortBy, sortDirection);

        Map<String, Object> response = new HashMap<>();

        response.put("data", result.getContent());
        response.put("total", result.getTotalElements());
        response.put("page", page);
        response.put("pages", result.getTotalPages());

        return ResponseEntity.ok(response);
    }

    @PreAuthorize("@permissionService.hasAccess('MANAGE_JOBAPPLICATIONS')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteApplication(@PathVariable String id) {
        jobApplicationService.deleteJobApplication(id);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Job application deleted successfully.");
        return ResponseEntity.ok(response);
    }
}
