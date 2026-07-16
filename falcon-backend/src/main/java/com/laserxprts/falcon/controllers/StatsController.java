package com.laserxprts.falcon.controllers;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.laserxprts.falcon.service.StatService;

@RestController
@RequestMapping("/api/stats")
public class StatsController {

    private final StatService statService;

    public StatsController(StatService statService) {
        this.statService = statService;
    }
    
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @GetMapping
    public ResponseEntity<Map<String, Long>> getStats() {
        return ResponseEntity.ok(statService.getStats());
    }
}
