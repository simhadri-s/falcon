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

import com.laserxprts.falcon.dto.request.IndustryRequest;
import com.laserxprts.falcon.dto.response.IndustryResponse;
import com.laserxprts.falcon.model.Industry;
import com.laserxprts.falcon.repository.IndustryRepository;

@ExtendWith(MockitoExtension.class)
public class IndustryServiceTest {

    @Mock private IndustryRepository industryRepository;
    @Mock private FileUploadService fileUploadService;

    @InjectMocks
    private IndustryService industryService;

    private Industry testIndustry;
    private IndustryRequest request;

    @BeforeEach
    void setUp() {
        testIndustry = new Industry();
        testIndustry.setId("IND-1");
        testIndustry.setName("Healthcare");
        testIndustry.setSlug("healthcare");

        request = new IndustryRequest();
        request.setName("Healthcare");
        request.setDescription("Health industry");
    }

    @Test
    void testCreateIndustry_Success() {
        when(industryRepository.existsBySlug("healthcare")).thenReturn(false);
        when(industryRepository.save(any(Industry.class))).thenReturn(testIndustry);

        IndustryResponse result = industryService.createIndustry(request);

        assertNotNull(result);
        assertEquals("healthcare", result.getSlug());
        verify(industryRepository).save(any(Industry.class));
    }

    @Test
    void testCreateIndustry_DuplicateSlug_ThrowsException() {
        when(industryRepository.existsBySlug("healthcare")).thenReturn(true);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> industryService.createIndustry(request));
        assertEquals("Industry or slug  already exitst", ex.getMessage());
        verify(industryRepository, never()).save(any(Industry.class));
    }
}
