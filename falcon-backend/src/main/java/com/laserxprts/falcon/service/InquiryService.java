package com.laserxprts.falcon.service;

import java.time.LocalDateTime;
import java.util.Objects;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.laserxprts.falcon.dto.response.InquiryResponse;
import com.laserxprts.falcon.model.Inquiry;
import com.laserxprts.falcon.repository.InquiryRepository;

@Service
public class InquiryService {
    
    private final InquiryRepository inquiryRepository;
    private final EmailService emailService;

    public InquiryService(InquiryRepository inquiryRepository, EmailService emailService) {
        this.emailService = emailService;
        this.inquiryRepository = inquiryRepository;
    }

    public InquiryResponse createInquiry(Inquiry inquiry) {
        if (inquiry == null) {
            throw new RuntimeException("Inquiry cannot be null");
        }

        inquiry.setCreatedAt(LocalDateTime.now());
        
        String subject = "Thank you for reaching out, " + inquiry.getName() + "!";
        
        String email = Objects.requireNonNull(inquiry.getEmail());
        String htmlBody = Objects.requireNonNull(createUserMailBody(inquiry));
        
        emailService.sendUserInquiryMail(email, subject, htmlBody);
        InquiryResponse response = InquiryResponse.from(inquiryRepository.save(inquiry));
        
        String companyName = emailService.getCompanyName();
        emailService.sendAdminInquiryMail(
            "Received new Inquiry on " + companyName, 
            Objects.requireNonNull(createAdminMailBody(inquiry))
        );
        
        return response;
    }

    public Page<InquiryResponse> getAllInquiry(int page,int limit, String sortBy, String sortDirection) {
        String sortField = (sortBy != null && !sortBy.isEmpty()) ? sortBy : "createdAt";
        Sort.Direction direction = Sort.Direction.DESC;
        if (sortDirection != null && sortDirection.equalsIgnoreCase("asc")) {
            direction = Sort.Direction.ASC;
        }
        Pageable pageable = PageRequest.of(Math.max(page-1, 0), limit, Sort.by(direction, sortField));

        return inquiryRepository.findAll(pageable)
            .map(InquiryResponse::from);
    }

    public Inquiry getById(String id) {
        if (id == null || id.isBlank()) {
            throw new RuntimeException("Id can not be null");
        }
        return inquiryRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Inquiry not found"));
    }

    public Inquiry updateStatus(String id, String status) {
        if (status == null || status.isBlank() || (!status.equals("READ") && !status.equals("RESPONDED"))) {
            throw new RuntimeException("Invalid status. Allowed values are: READ, RESPONDED");
        }
        
        Inquiry inquiry = getById(id);

        inquiry.setStatus(status);

        return inquiryRepository.save(inquiry);
    }

    private String createUserMailBody(Inquiry inquiry) {
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
                                            Thank you for reaching out to us. We have successfully received your inquiry and our team is already reviewing it.
                                        </p>
                                        
                                        <div style="background-color: #f8f9fa; border-left: 4px solid #007bff; padding: 15px; margin-bottom: 20px;">
                                            <p style="margin: 0; font-size: 15px; color: #555;">
                                                <strong>Regarding:</strong> %s
                                            </p>
                                        </div>
                                        
                                        <p style="line-height: 1.6; font-size: 16px; margin-bottom: 30px;">
                                            One of our experts will get back to you at this email address very soon. 
                                        </p>
                                        
                                        <table cellpadding="0" cellspacing="0">
                                            <tr>
                                                <td style="border-radius: 4px; background-color: #007bff; text-align: center;">
                                                    <a href="#" style="display: block; padding: 12px 24px; color: #ffffff; text-decoration: none; font-weight: bold; font-size: 16px;">Visit Our Website</a>
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
            """.formatted(emailService.getCompanyName(), inquiry.getName(), inquiry.getSubject(), emailService.getCompanyName());
    }

    private String createAdminMailBody(Inquiry inquiry) {
        String displayMessage = (inquiry.getMessage() != null && !inquiry.getMessage().isBlank()) 
                                ? inquiry.getMessage() 
                                : "<i>No additional message provided.</i>";
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
            </head>
            <body style="margin: 0; padding: 0; font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif; background-color: #e9ecef; color: #333333;">
                <table width="100%%" cellpadding="0" cellspacing="0" style="background-color: #e9ecef; padding: 40px 0;">
                    <tr>
                        <td align="center">
                            <table width="600" cellpadding="0" cellspacing="0" style="background-color: #ffffff; border-radius: 8px; overflow: hidden; box-shadow: 0 4px 15px rgba(0,0,0,0.1); border-top: 5px solid #dc3545;">
                                
                                <tr>
                                    <td style="padding: 20px 30px; background-color: #f8f9fa; border-bottom: 1px solid #eeeeee;">
                                        <h2 style="margin: 0; color: #dc3545; font-size: 20px;">New Inquiry Received</h2>
                                    </td>
                                </tr>
                                
                                <tr>
                                    <td style="padding: 30px;">
                                        <p style="margin-top: 0; font-size: 15px; line-height: 1.5; margin-bottom: 25px;">
                                            A new contact form has been submitted on the %s website. Here are the details:
                                        </p>
                                        
                                        <table width="100%%" cellpadding="12" cellspacing="0" style="background-color: #f8f9fa; border-radius: 6px; border: 1px solid #eeeeee; margin-bottom: 25px;">
                                            <tr>
                                                <td width="25%%" style="color: #666666; font-size: 14px; border-bottom: 1px solid #e0e0e0;"><strong>Name:</strong></td>
                                                <td style="font-size: 15px; border-bottom: 1px solid #e0e0e0; color: #111111;">%s</td>
                                            </tr>
                                            <tr>
                                                <td style="color: #666666; font-size: 14px; border-bottom: 1px solid #e0e0e0;"><strong>Email:</strong></td>
                                                <td style="font-size: 15px; border-bottom: 1px solid #e0e0e0;"><a href="mailto:%s" style="color: #007bff; text-decoration: none;">%s</a></td>
                                            </tr>
                                            <tr>
                                                <td style="color: #666666; font-size: 14px; border-bottom: 1px solid #e0e0e0;"><strong>Subject:</strong></td>
                                                <td style="font-size: 15px; border-bottom: 1px solid #e0e0e0; color: #111111;">%s</td>
                                            </tr>
                                            <tr>
                                                <td style="color: #666666; font-size: 14px;"><strong>Message:</strong></td>
                                                <td style="font-size: 15px; color: #111111; line-height: 1.5;">%s</td>
                                            </tr>
                                        </table>
                                        
                                        <table cellpadding="0" cellspacing="0" width="100%%">
                                            <tr>
                                                <td align="center">
                                                    <a href="mailto:%s?subject=Re: %s" style="display: inline-block; padding: 12px 24px; background-color: #28a745; color: #ffffff; text-decoration: none; font-weight: bold; border-radius: 4px; font-size: 15px;">Reply to Customer Directly</a>
                                                </td>
                                            </tr>
                                        </table>
                                    </td>
                                </tr>
                                
                                <tr>
                                    <td style="background-color: #f8f9fa; padding: 15px 30px; text-align: center; border-top: 1px solid #eeeeee;">
                                        <p style="margin: 0; font-size: 12px; color: #888888;">Automated System Alert &bull; %s Backend</p>
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
                inquiry.getName(),
                inquiry.getEmail(),
                inquiry.getEmail(),
                inquiry.getSubject(),
                displayMessage,
                inquiry.getEmail(),
                inquiry.getSubject(),
                emailService.getCompanyName()
            );
    }
}