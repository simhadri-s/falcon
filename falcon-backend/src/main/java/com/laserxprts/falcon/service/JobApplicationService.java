package com.laserxprts.falcon.service;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.laserxprts.falcon.dto.request.JobApplicationRequest;
import com.laserxprts.falcon.dto.response.JobApplicationResponse;
import com.laserxprts.falcon.model.JobApplication;
import com.laserxprts.falcon.model.Job;
import com.laserxprts.falcon.repository.JobApplicationRepository;
import com.laserxprts.falcon.repository.JobRepository;

@Service
public class JobApplicationService {

    private final JobApplicationRepository jobApplicationRepository;
    private final FileUploadService fileUploadService;
    private final JobRepository jobRepository;
    private final EmailService emailService;

    public JobApplicationService(JobApplicationRepository jobApplicationRepository,
                                 FileUploadService fileUploadService,
                                 JobRepository jobRepository,
                                 EmailService emailService
                                ) {
        this.jobApplicationRepository = jobApplicationRepository;
        this.fileUploadService = fileUploadService;
        this.jobRepository = jobRepository;
        this.emailService = emailService;
    }

    public JobApplicationResponse createJobApplication(JobApplicationRequest application) {

        JobApplication jobApplication = new JobApplication();

        jobApplication.setCvUrl(uploadFile(application.getCv()));
        jobApplication.setCoverLetterUrl(uploadFile(application.getCoverLetter()));
        
        jobApplication.setName(application.getName());
        jobApplication.setEmail(application.getEmail());
        jobApplication.setPhone(application.getPhone());
        Job job = jobRepository.findById(application.getJobId())
            .orElseThrow(() -> new RuntimeException("Job not found with job Id"));
        jobApplication.setJob(job);

        JobApplicationResponse applicationResponse = JobApplicationResponse.from(jobApplicationRepository.save(jobApplication));

        String companyName = emailService.getCompanyName();
        String subject = "Application Received – " + job.getTitle() + " | " + companyName;
        String htmlBody = Objects.requireNonNull(createJobApplicantMailBody(jobApplication));

        emailService.sendJobApplicationMail(application.getEmail(), subject, htmlBody);

        return applicationResponse;
    }

    private String uploadFile(MultipartFile file) {
        if (file != null && !file.isEmpty()) {
            try {
                if (!"application/pdf".equals(file.getContentType())) {
                    throw new IllegalArgumentException("Only PDF files are allowed");
                }
                return fileUploadService.uploadPdf(file);
            } catch (Exception e) {
                throw new RuntimeException("Failed to upload PDF", e);
            }
        }
        return null;
    }

    public Page<JobApplicationResponse> getAllApplications(int page, int limit, String sortBy, String sortDirection) {
        String sortField = (sortBy != null && !sortBy.isEmpty()) ? sortBy : "createdAt";
        Sort.Direction direction = Sort.Direction.DESC;
        if (sortDirection != null && sortDirection.equalsIgnoreCase("asc")) {
            direction = Sort.Direction.ASC;
        }

        Pageable pageable = PageRequest.of(
                Math.max(page-1, 0),
                limit,
                Sort.by(direction, sortField)
        );

        return jobApplicationRepository.findAll(pageable)
        .map(JobApplicationResponse::from);
    }

    public void deleteJobApplication(String id) {
        JobApplication jobApplication = jobApplicationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Job Application not found with Id: " + id));

        List<String> urlsToDelete = new ArrayList<>();
        if (jobApplication.getCvUrl() != null && !jobApplication.getCvUrl().isEmpty()) {
            urlsToDelete.add(jobApplication.getCvUrl());
        }
        if (jobApplication.getCoverLetterUrl() != null && !jobApplication.getCoverLetterUrl().isEmpty()) {
            urlsToDelete.add(jobApplication.getCoverLetterUrl());
        }

        if (!urlsToDelete.isEmpty()) {
            fileUploadService.deleteFiles(urlsToDelete);
        }

        jobApplicationRepository.delete(jobApplication);
    }

    private String createJobApplicantMailBody(JobApplication jobApplication) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
            </head>
            <body style="margin: 0; padding: 0; font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif; background-color: #f4f5f7; color: #333333;">
                <table width="100%%" cellpadding="0" cellspacing="0" style="background-color: #f4f5f7; padding: 40px 0;">
                    <tr>
                        <td align="center">
                            <table width="600" cellpadding="0" cellspacing="0" style="background-color: #ffffff; border-radius: 8px; overflow: hidden; box-shadow: 0 4px 10px rgba(0,0,0,0.1);">
                                
                                <tr>
                                    <td style="background-color: #1a1a1a; padding: 30px; text-align: center;">
                                        <h1 style="color: #ffffff; margin: 0; font-size: 24px; letter-spacing: 2px; text-transform: uppercase;">%s</h1>
                                    </td>
                                </tr>
                                
                                <tr>
                                    <td style="padding: 40px 30px;">
                                        <h2 style="margin-top: 0; color: #1a1a1a; font-size: 20px;">Hello %s,</h2>
                                        
                                        <p style="line-height: 1.6; font-size: 16px; margin-bottom: 20px;">
                                            Thank you for applying at %s. We have successfully received your application and our hiring team is currently reviewing it.
                                        </p>
                                        
                                        <div style="background-color: #f8f9fa; border-left: 4px solid #007bff; padding: 15px; margin-bottom: 16px;">
                                            <p style="margin: 0 0 8px 0; font-size: 15px; color: #555;">
                                                <strong>Position Applied:</strong> %s
                                            </p>
                                            <p style="margin: 0 0 8px 0; font-size: 15px; color: #555;">
                                                <strong>Application Date:</strong> %s
                                            </p>
                                            <p style="margin: 0; font-size: 15px; color: #555;">
                                                <strong>Application ID:</strong> #%s
                                            </p>
                                        </div>
                                        
                                        <p style="line-height: 1.6; font-size: 16px; margin-bottom: 30px;">
                                            If your profile matches our requirements, one of our HR representatives will reach out to you at this email address to discuss the next steps. We appreciate your patience.
                                        </p>
                                        
                                        <table cellpadding="0" cellspacing="0">
                                            <tr>
                                                <td style="border-radius: 4px; background-color: #007bff; text-align: center;">
                                                    <a href="https://laserexperts.ae/" style="display: block; padding: 12px 24px; color: #ffffff; text-decoration: none; font-weight: bold; font-size: 16px;">Visit Our Website</a>
                                                </td>
                                            </tr>
                                        </table>
                                    </td>
                                </tr>
                                
                                <tr>
                                    <td style="background-color: #f8f9fa; padding: 20px 30px; text-align: center; border-top: 1px solid #eeeeee;">
                                        <p style="margin: 0; font-size: 13px; color: #888888; line-height: 1.5;">
                                            &copy; 2026 %s. All rights reserved.<br>
                                            Hosur, Chennai, Bengaluru, UAE
                                        </p>
                                    </td>
                                </tr>
                                
                            </table>
                        </td>
                    </tr>
                </table>
            </body>
            </html>
            """.formatted(
                emailService.getCompanyName(),
                jobApplication.getName(),
                emailService.getCompanyName(),
                jobApplication.getJob().getTitle(),
                jobApplication.getCreatedAt().format(DateTimeFormatter.ofPattern("dd MMM yyyy")),
                jobApplication.getId(),
                emailService.getCompanyName()
            );
    }
}