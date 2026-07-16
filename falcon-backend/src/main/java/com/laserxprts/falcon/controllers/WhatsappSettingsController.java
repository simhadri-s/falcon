package com.laserxprts.falcon.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.laserxprts.falcon.model.WhatsappSettings;
import com.laserxprts.falcon.repository.WhatsappSettingsRepository;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/whatsapp-settings")
@RequiredArgsConstructor
public class WhatsappSettingsController {

    private final WhatsappSettingsRepository whatsappSettingsRepository;

    @GetMapping
    public ResponseEntity<WhatsappSettings> getSettings() {
        return whatsappSettingsRepository.findById("WHATSAPP_SETTINGS")
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.ok(new WhatsappSettings("WHATSAPP_SETTINGS", "", "")));
    }

    @PutMapping
    public ResponseEntity<WhatsappSettings> updateSettings(@RequestBody WhatsappSettings settings) {
        settings.setId("WHATSAPP_SETTINGS");
        WhatsappSettings saved = whatsappSettingsRepository.save(settings);
        return ResponseEntity.ok(saved);
    }
}
