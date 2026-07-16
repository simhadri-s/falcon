package com.laserxprts.falcon.service;

import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

import com.laserxprts.falcon.model.MailerSettings;
import com.laserxprts.falcon.repository.MailerSettingsRepository;

@Service
@RequiredArgsConstructor
public class MailerSettingsService {

    private final MailerSettingsRepository repository;
    private static final String SETTINGS_ID = "MAILER_SETTINGS";

    public MailerSettings getSettings() {
        return repository.findById(SETTINGS_ID)
                .orElseGet(() -> {
                    MailerSettings defaultSettings = new MailerSettings();
                    defaultSettings.setId(SETTINGS_ID);
                    defaultSettings.setMailHost("smtp.gmail.com");
                    defaultSettings.setMailPort(587);
                    defaultSettings.setMailUsername("");
                    defaultSettings.setMailPassword("");
                    return defaultSettings;
                });
    }

    public MailerSettings initializeSettings(MailerSettings settings) {
        settings.setId(SETTINGS_ID);
        return repository.save(settings);
    }

    public MailerSettings updateSettings(MailerSettings settings) {
        settings.setId(SETTINGS_ID);
        return repository.save(settings);
    }

    public void deleteSettings() {
        repository.deleteById(SETTINGS_ID);
    }
}
