package com.laserxprts.falcon.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.laserxprts.falcon.model.MailerSettings;
import com.laserxprts.falcon.service.MailerSettingsService;

import lombok.RequiredArgsConstructor;

@PreAuthorize("@permissionService.hasAccess('MANAGE_COMPANYSETTINGS')")
@RestController
@RequestMapping("/api/mailer-settings")
@RequiredArgsConstructor
public class MailerSettingsController {

    private final MailerSettingsService service;

    @GetMapping
    public ResponseEntity<MailerSettings> getSettings() {
        return ResponseEntity.ok(service.getSettings());
    }

    @PostMapping
    public ResponseEntity<MailerSettings> initializeSettings(@RequestBody MailerSettings settings) {
        return ResponseEntity.ok(service.initializeSettings(settings));
    }

    @PutMapping
    public ResponseEntity<MailerSettings> updateSettings(@RequestBody MailerSettings settings) {
        return ResponseEntity.ok(service.updateSettings(settings));
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteSettings() {
        service.deleteSettings();
        return ResponseEntity.noContent().build();
    }
}
