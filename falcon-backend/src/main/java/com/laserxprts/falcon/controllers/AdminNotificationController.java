package com.laserxprts.falcon.controllers;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.laserxprts.falcon.dto.request.NotificationRequest;
import com.laserxprts.falcon.service.FcmService;
import com.laserxprts.falcon.service.FileUploadService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/notifications")
@RequiredArgsConstructor
public class AdminNotificationController {

    private final FcmService fcmService;
    private final FileUploadService fileUploadService;

    @PostMapping("/broadcast")
    @PreAuthorize("@permissionService.hasAccess('MANAGE_NOTIFICATIONS')")
    public ResponseEntity<Map<String, String>> broadcastNotification(@Valid @ModelAttribute NotificationRequest request) {
        String imageUrl = request.getImageUrl();
        
        if (request.getImage() != null && !request.getImage().isEmpty()) {
            imageUrl = fileUploadService.uploadImage(request.getImage());
        }

        fcmService.broadcastNotification(
            request.getTitle(),
            request.getBody(),
            imageUrl,
            request.getData()
        );
        return ResponseEntity.ok(Map.of("message", "Broadcast notification sent and stored."));
    }
}
