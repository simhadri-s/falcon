package com.laserxprts.falcon.service;

import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

import com.laserxprts.falcon.model.CompanySettings;
import com.laserxprts.falcon.repository.CompanySettingsRepository;

@Service
@RequiredArgsConstructor
public class CompanySettingsService {

    private final CompanySettingsRepository repository;
    private static final String SETTINGS_ID = "COMPANY_SETTINGS";

    public CompanySettings getSettings() {
        return repository.findById(SETTINGS_ID)
                .orElseThrow(() -> new IllegalArgumentException("Company settings not found"));
    }

    public CompanySettings initializeSettings(CompanySettings settings) {
        CompanySettings merged = mergeSettings(repository.findById(SETTINGS_ID).orElse(new CompanySettings()), settings);
        merged.setId(SETTINGS_ID);
        return repository.save(merged);
    }

    public CompanySettings updateSettings(CompanySettings settings) {
        CompanySettings merged = mergeSettings(repository.findById(SETTINGS_ID).orElse(new CompanySettings()), settings);
        merged.setId(SETTINGS_ID);
        return repository.save(merged);
    }

    public void deleteSettings() {
        repository.deleteById(SETTINGS_ID);
    }

    private CompanySettings mergeSettings(CompanySettings existing, CompanySettings incoming) {
        CompanySettings merged = new CompanySettings();
        merged.setId(SETTINGS_ID);
        merged.setCompanyName(incoming.getCompanyName() != null ? incoming.getCompanyName() : existing.getCompanyName());
        merged.setEmail(incoming.getEmail() != null ? incoming.getEmail() : existing.getEmail());
        merged.setPhone(incoming.getPhone() != null ? incoming.getPhone() : existing.getPhone());
        merged.setAddress(incoming.getAddress() != null ? incoming.getAddress() : existing.getAddress());
        merged.setLogoUrl(incoming.getLogoUrl() != null ? incoming.getLogoUrl() : existing.getLogoUrl());
        merged.setWorkingHours(incoming.getWorkingHours() != null ? incoming.getWorkingHours() : existing.getWorkingHours());
        merged.setReturnWindowDays(incoming.getReturnWindowDays() != null ? incoming.getReturnWindowDays() : existing.getReturnWindowDays());
        merged.setTermsAndConditions(incoming.getTermsAndConditions() != null ? incoming.getTermsAndConditions() : existing.getTermsAndConditions());
        merged.setPrivacyPolicy(incoming.getPrivacyPolicy() != null ? incoming.getPrivacyPolicy() : existing.getPrivacyPolicy());
        merged.setOrderIdPrefix(incoming.getOrderIdPrefix() != null ? incoming.getOrderIdPrefix() : existing.getOrderIdPrefix());
        return merged;
    }
}
