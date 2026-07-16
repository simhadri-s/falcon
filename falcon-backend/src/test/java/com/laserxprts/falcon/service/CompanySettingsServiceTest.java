package com.laserxprts.falcon.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.laserxprts.falcon.model.CompanySettings;
import com.laserxprts.falcon.repository.CompanySettingsRepository;

@ExtendWith(MockitoExtension.class)
class CompanySettingsServiceTest {

    @Mock
    private CompanySettingsRepository repository;

    @InjectMocks
    private CompanySettingsService companySettingsService;

    @Test
    void updateSettingsPreservesExistingLogoAndOptionalFieldsWhenIncomingValuesAreNull() {
        CompanySettings existing = new CompanySettings();
        existing.setId("COMPANY_SETTINGS");
        existing.setCompanyName("Falcon");
        existing.setLogoUrl("https://cdn.example/logo.png");
        existing.setOrderIdPrefix("FAL");
        existing.setReturnWindowDays(7);

        CompanySettings incoming = new CompanySettings();
        incoming.setCompanyName("Falcon Updated");

        when(repository.findById("COMPANY_SETTINGS")).thenReturn(Optional.of(existing));
        when(repository.save(any(CompanySettings.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CompanySettings saved = companySettingsService.updateSettings(incoming);

        assertEquals("COMPANY_SETTINGS", saved.getId());
        assertEquals("Falcon Updated", saved.getCompanyName());
        assertEquals("https://cdn.example/logo.png", saved.getLogoUrl());
        assertEquals("FAL", saved.getOrderIdPrefix());
        assertEquals(7, saved.getReturnWindowDays());
    }

    @Test
    void initializeSettingsCreatesNewEntryWhenNothingExists() {
        CompanySettings incoming = new CompanySettings();
        incoming.setCompanyName("Falcon");

        when(repository.findById("COMPANY_SETTINGS")).thenReturn(Optional.empty());
        when(repository.save(any(CompanySettings.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CompanySettings saved = companySettingsService.initializeSettings(incoming);

        assertEquals("COMPANY_SETTINGS", saved.getId());
        assertEquals("Falcon", saved.getCompanyName());
        assertNull(saved.getLogoUrl());
    }
}
