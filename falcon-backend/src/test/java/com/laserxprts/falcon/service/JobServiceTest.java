package com.laserxprts.falcon.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.laserxprts.falcon.dto.response.JobResponse;
import com.laserxprts.falcon.model.Job;
import com.laserxprts.falcon.repository.JobRepository;
import com.laserxprts.falcon.repository.JobApplicationRepository;

@ExtendWith(MockitoExtension.class)
public class JobServiceTest {

    @Mock private JobRepository jobRepository;
    @Mock private JobApplicationRepository jobApplicationRepository;
    @Mock private FileUploadService fileUploadService;

    @InjectMocks
    private JobService jobService;

    private Job testJob;

    @BeforeEach
    void setUp() {
        testJob = new Job();
        testJob.setId("JOB-1");
        testJob.setTitle("Software Engineer");
    }

    @Test
    void testCreateJob_Success() {
        when(jobRepository.save(any(Job.class))).thenReturn(testJob);

        JobResponse result = jobService.createJob(testJob);

        assertNotNull(result);
        assertEquals("Software Engineer", result.getTitle());
        verify(jobRepository).save(any(Job.class));
    }

    @Test
    void testDeleteJob_Success() {
        when(jobRepository.existsById("JOB-1")).thenReturn(true);

        jobService.deleteJob("JOB-1");

        verify(jobApplicationRepository).deleteByJobId("JOB-1");
        verify(jobRepository).deleteById("JOB-1");
    }
}
