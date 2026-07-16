package com.laserxprts.falcon.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.laserxprts.falcon.model.CompanySettings;
import com.laserxprts.falcon.service.CompanySettingsService;

import lombok.RequiredArgsConstructor;

@PreAuthorize("@permissionService.hasAccess('MANAGE_COMPANYSETTINGS')")
@RestController
@RequestMapping("/api/company-settings")
@RequiredArgsConstructor
public class CompanySettingsController {

    private final CompanySettingsService service;

    @PreAuthorize("permitAll()")
    @GetMapping
    public ResponseEntity<CompanySettings> getSettings() {
        return ResponseEntity.ok(service.getSettings());
    }

    @PostMapping
    public ResponseEntity<CompanySettings> initializeSettings(@RequestBody CompanySettings settings) {
        return ResponseEntity.ok(service.initializeSettings(settings));
    }

    @PutMapping
    public ResponseEntity<CompanySettings> updateSettings(@RequestBody CompanySettings settings) {
        return ResponseEntity.ok(service.updateSettings(settings));
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteSettings() {
        service.deleteSettings();
        return ResponseEntity.noContent().build();
    }
}
