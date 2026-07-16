package com.laserxprts.falcon.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import com.laserxprts.falcon.dto.request.JobApplicationRequest;
import com.laserxprts.falcon.dto.response.JobApplicationResponse;
import com.laserxprts.falcon.model.Job;
import com.laserxprts.falcon.model.JobApplication;
import com.laserxprts.falcon.repository.JobApplicationRepository;
import com.laserxprts.falcon.repository.JobRepository;

@ExtendWith(MockitoExtension.class)
public class JobApplicationServiceTest {

    @Mock private JobApplicationRepository jobApplicationRepository;
    @Mock private FileUploadService fileUploadService;
    @Mock private JobRepository jobRepository;
    @Mock private EmailService emailService;

    @InjectMocks
    private JobApplicationService jobApplicationService;

    private JobApplicationRequest request;
    private Job testJob;

    @BeforeEach
    void setUp() {
        testJob = new Job();
        testJob.setId("JOB-1");
        testJob.setTitle("Engineer");

        request = new JobApplicationRequest();
        request.setJobId("JOB-1");
        request.setName("John Doe");
        request.setEmail("john@doe.com");
    }

    @Test
    void testCreateJobApplication_Success() {
        when(jobRepository.findById("JOB-1")).thenReturn(Optional.of(testJob));
        when(emailService.getCompanyName()).thenReturn("Falcon");

        JobApplication mockApp = new JobApplication();
        mockApp.setId("APP-1");
        mockApp.setName("John Doe");
        when(jobApplicationRepository.save(any(JobApplication.class))).thenAnswer(invocation -> {
            JobApplication savedApp = invocation.getArgument(0);
            savedApp.setId("APP-1");
            savedApp.setCreatedAt(java.time.LocalDateTime.now());
            return savedApp;
        });

        JobApplicationResponse result = jobApplicationService.createJobApplication(request);

        assertNotNull(result);
        assertEquals("John Doe", result.getName());
        verify(jobApplicationRepository).save(any(JobApplication.class));
        verify(emailService).sendJobApplicationMail(eq("john@doe.com"), anyString(), anyString());
    }

    @Test
    void testCreateJobApplication_JobNotFound_ThrowsException() {
        when(jobRepository.findById("JOB-1")).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class, () -> jobApplicationService.createJobApplication(request));
        assertEquals("Job not found with job Id", ex.getMessage());
        verify(jobApplicationRepository, never()).save(any());
    }
}
