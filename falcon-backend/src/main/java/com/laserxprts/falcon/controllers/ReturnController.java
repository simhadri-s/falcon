package com.laserxprts.falcon.controllers;

import java.security.Principal;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.laserxprts.falcon.dto.request.ReturnCreateRequest;
import com.laserxprts.falcon.enums.ReturnStatus;
import com.laserxprts.falcon.model.ReturnRequest;
import com.laserxprts.falcon.service.ReturnService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/returns")
@RequiredArgsConstructor
public class ReturnController {

    private final ReturnService returnService;

    @PostMapping
    public ResponseEntity<ReturnRequest> createReturnRequest(Principal principal, @RequestBody ReturnCreateRequest request) {
        return ResponseEntity.ok(returnService.createReturnRequest(principal.getName(), request));
    }

    @GetMapping("/my")
    public ResponseEntity<List<ReturnRequest>> getMyReturns(Principal principal) {
        return ResponseEntity.ok(returnService.getReturnsByUserId(principal.getName()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReturnRequest> getReturnById(@PathVariable String id) {
        return ResponseEntity.ok(returnService.getReturnById(id));
    }

    // Admin Endpoints
    @GetMapping
    public ResponseEntity<List<ReturnRequest>> getAllReturns() {
        return ResponseEntity.ok(returnService.getAllReturns());
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ReturnRequest> updateReturnStatus(
            @PathVariable String id,
            @RequestParam ReturnStatus status,
            @RequestParam(required = false) String adminComment) {
        return ResponseEntity.ok(returnService.updateReturnStatus(id, status, adminComment));
    }
}
