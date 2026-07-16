package com.laserxprts.falcon.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.laserxprts.falcon.model.MailerSettings;
import com.laserxprts.falcon.repository.MailerSettingsRepository;

@ExtendWith(MockitoExtension.class)
public class MailerSettingsServiceTest {

    @Mock private MailerSettingsRepository repository;

    @InjectMocks
    private MailerSettingsService service;

    private MailerSettings testSettings;

    @BeforeEach
    void setUp() {
        testSettings = new MailerSettings();
        testSettings.setId("MAILER_SETTINGS");
        testSettings.setMailHost("smtp.test.com");
    }

    @Test
    void testGetSettings_Existing() {
        when(repository.findById("MAILER_SETTINGS")).thenReturn(Optional.of(testSettings));

        MailerSettings result = service.getSettings();

        assertNotNull(result);
        assertEquals("smtp.test.com", result.getMailHost());
    }

    @Test
    void testGetSettings_DefaultFallback() {
        when(repository.findById("MAILER_SETTINGS")).thenReturn(Optional.empty());

        MailerSettings result = service.getSettings();

        assertNotNull(result);
        assertEquals("smtp.gmail.com", result.getMailHost());
        assertEquals(587, result.getMailPort());
    }

    @Test
    void testInitializeSettings() {
        when(repository.save(any(MailerSettings.class))).thenReturn(testSettings);

        MailerSettings newSettings = new MailerSettings();
        MailerSettings result = service.initializeSettings(newSettings);

        assertNotNull(result);
        verify(repository).save(newSettings);
    }
}
