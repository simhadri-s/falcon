package com.laserxprts.falcon.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import com.laserxprts.falcon.model.Banner;
import com.laserxprts.falcon.repository.BannerRepository;

@ExtendWith(MockitoExtension.class)
public class BannerServiceTest {

    @Mock private BannerRepository bannerRepository;
    @Mock private FileUploadService fileUploadService;

    @InjectMocks
    private BannerService bannerService;

    private Banner testBanner;
    private MultipartFile testFile;

    @BeforeEach
    void setUp() {
        testBanner = new Banner();
        testBanner.setTitle("Sale");

        testFile = new MockMultipartFile("file", "test.jpg", "image/jpeg", "test data".getBytes());
    }

    @Test
    void testCreateBanner_Success() {
        when(fileUploadService.uploadImage(any(MultipartFile.class))).thenReturn("http://image.url");
        when(bannerRepository.save(any(Banner.class))).thenReturn(testBanner);

        Banner result = bannerService.create(testBanner, testFile);

        assertNotNull(result);
        assertEquals("http://image.url", testBanner.getImageUrl());
        verify(bannerRepository).save(any(Banner.class));
    }

    @Test
    void testCreateBanner_NullImage_ThrowsException() {
        RuntimeException ex = assertThrows(RuntimeException.class, () -> bannerService.create(testBanner, null));
        assertEquals("Image cannot be null", ex.getMessage());
        verify(bannerRepository, never()).save(any(Banner.class));
    }
}
