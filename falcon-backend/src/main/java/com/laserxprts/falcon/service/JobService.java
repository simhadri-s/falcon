package com.laserxprts.falcon.service;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.laserxprts.falcon.dto.response.JobResponse;
import com.laserxprts.falcon.model.Job;
import com.laserxprts.falcon.repository.JobRepository;
import com.laserxprts.falcon.repository.JobApplicationRepository;
import com.laserxprts.falcon.model.JobApplication;
import org.springframework.lang.NonNull;
import java.util.List;
import java.util.ArrayList;


@Service
public class JobService {

    private final JobRepository jobRepository;
    private final JobApplicationRepository jobApplicationRepository;
    private final FileUploadService fileUploadService;

    public JobService(JobRepository jobRepository, JobApplicationRepository jobApplicationRepository, FileUploadService fileUploadService) {
        this.jobRepository = jobRepository;
        this.jobApplicationRepository = jobApplicationRepository;
        this.fileUploadService = fileUploadService;
    }

    public Page<JobResponse> getLatestJobs(int page, int limit, String sortBy, String sortDirection, boolean canViewInactive) {
        String sortField = (sortBy != null && !sortBy.isEmpty()) ? sortBy : "id";
        Sort.Direction direction = Sort.Direction.ASC;
        if (sortDirection != null && sortDirection.equalsIgnoreCase("desc")) {
            direction = Sort.Direction.DESC;
        }
        Pageable pageable = PageRequest.of(Math.max(page-1, 0), limit, Sort.by(direction, sortField)); 
        if (canViewInactive) {
            return jobRepository.findAll(pageable)
                .map(JobResponse::from);
        } else {
            return jobRepository.getByActiveTrue(pageable)
                .map(JobResponse::from);
        }
    }
    
    public JobResponse createJob(@NonNull Job job) {
        return JobResponse.from(jobRepository.save(job));
    }

    public JobResponse updateJob(String id, Job updatedJob) {
        if (id == null || id.isBlank()) {
            throw new RuntimeException("ID can not be null");
        }
        
        Job existingJob = jobRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Can not find the job"));

        // Upgraded to prevent empty space updates
        if (updatedJob.getTitle() != null && !updatedJob.getTitle().isBlank()) {
            existingJob.setTitle(updatedJob.getTitle());
        }
        if (updatedJob.getDepartment() != null && !updatedJob.getDepartment().isBlank()) {
            existingJob.setDepartment(updatedJob.getDepartment());
        }
        if (updatedJob.getLocation() != null && !updatedJob.getLocation().isBlank()) {
            existingJob.setLocation(updatedJob.getLocation());
        }
        if (updatedJob.getType() != null && !updatedJob.getType().isBlank()) {
            existingJob.setType(updatedJob.getType());
        }
        existingJob.setActive(updatedJob.isActive());

        return JobResponse.from(jobRepository.save(existingJob));
    }

    public void deleteJob(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Invalid job ID");
        }

        if (!jobRepository.existsById(id)) {
            throw new RuntimeException("Job not found");
        }
        
        List<JobApplication> apps = jobApplicationRepository.findByJobId(id);
        List<String> urlsToDelete = new ArrayList<>();
        for (JobApplication app : apps) {
            if (app.getCvUrl() != null && !app.getCvUrl().isBlank()) urlsToDelete.add(app.getCvUrl());
            if (app.getCoverLetterUrl() != null && !app.getCoverLetterUrl().isBlank()) urlsToDelete.add(app.getCoverLetterUrl());
        }
        if (!urlsToDelete.isEmpty()) {
            fileUploadService.deleteFiles(urlsToDelete);
        }

        jobApplicationRepository.deleteByJobId(id);
        jobRepository.deleteById(id);
    }
}
