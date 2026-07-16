package com.laserxprts.falcon.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import com.laserxprts.falcon.dto.request.CompanyImageRequest;
import com.laserxprts.falcon.exception.ApiException;
import com.laserxprts.falcon.model.CompanyImage;
import com.laserxprts.falcon.repository.CompanyImageRepository;
import com.laserxprts.falcon.repository.CompanySettingsRepository;

@ExtendWith(MockitoExtension.class)
public class CompanyImageServiceTest {

    @Mock private CompanyImageRepository companyImageRepository;
    @Mock private CompanySettingsRepository companySettingsRepository;
    @Mock private FileUploadService fileUploadService;

    @InjectMocks
    private CompanyImageService companyImageService;

    private CompanyImageRequest request;

    @BeforeEach
    void setUp() {
        request = new CompanyImageRequest();
        request.setName("Falcon HQ");
        request.setLogo(new MockMultipartFile("logo", "logo.jpg", "image/jpeg", "test".getBytes()));
    }

    @Test
    void testCreateCompanyImage_Success() {
        when(companyImageRepository.findAll()).thenReturn(Collections.emptyList());
        when(fileUploadService.uploadImage(any())).thenReturn("logo.url");
        
        CompanyImage savedImage = new CompanyImage();
        savedImage.setId("IMG-1");
        savedImage.setLogoUrl("logo.url");
        when(companyImageRepository.save(any(CompanyImage.class))).thenReturn(savedImage);

        CompanyImage result = companyImageService.createCompanyImage(request);

        assertNotNull(result);
        assertEquals("logo.url", result.getLogoUrl());
        verify(companyImageRepository).save(any(CompanyImage.class));
    }

    @Test
    void testCreateCompanyImage_Conflict_ThrowsException() {
        when(companyImageRepository.findAll()).thenReturn(List.of(new CompanyImage()));

        ApiException ex = assertThrows(ApiException.class, () -> companyImageService.createCompanyImage(request));
        assertTrue(ex.getMessage().contains("A company image already exists"));
        verify(companyImageRepository, never()).save(any());
    }
}
