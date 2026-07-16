package com.laserxprts.falcon.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.laserxprts.falcon.dto.response.InquiryResponse;
import com.laserxprts.falcon.model.Inquiry;
import com.laserxprts.falcon.repository.InquiryRepository;

@ExtendWith(MockitoExtension.class)
public class InquiryServiceTest {

    @Mock private InquiryRepository inquiryRepository;
    @Mock private EmailService emailService;

    @InjectMocks
    private InquiryService inquiryService;

    private Inquiry testInquiry;

    @BeforeEach
    void setUp() {
        testInquiry = new Inquiry();
        testInquiry.setId("INQ-1");
        testInquiry.setName("John Doe");
        testInquiry.setEmail("john@doe.com");
        testInquiry.setMessage("I have a question about shipping.");
    }

    @Test
    void testCreateInquiry_Success() {
        when(inquiryRepository.save(any(Inquiry.class))).thenReturn(testInquiry);
        when(emailService.getCompanyName()).thenReturn("Falcon");

        InquiryResponse result = inquiryService.createInquiry(testInquiry);

        assertNotNull(result);
        assertEquals("John Doe", result.getName());
        verify(inquiryRepository).save(any(Inquiry.class));
        verify(emailService).sendUserInquiryMail(eq("john@doe.com"), anyString(), anyString());
        verify(emailService).sendAdminInquiryMail(anyString(), anyString());
    }

    @Test
    void testCreateInquiry_NullInquiry_ThrowsException() {
        RuntimeException ex = assertThrows(RuntimeException.class, () -> inquiryService.createInquiry(null));
        assertEquals("Inquiry cannot be null", ex.getMessage());
        verify(inquiryRepository, never()).save(any(Inquiry.class));
    }
}
