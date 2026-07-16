package com.laserxprts.falcon.controllers;

import java.util.HashMap;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.lang.NonNull;
import org.springframework.security.access.prepost.PreAuthorize;

import com.laserxprts.falcon.exception.ApiException;
import com.laserxprts.falcon.dto.response.InquiryResponse;
import com.laserxprts.falcon.model.Inquiry;
import com.laserxprts.falcon.service.InquiryService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/inquiries")
public class InquiryController {

    private final InquiryService inquiryService;

    public InquiryController(InquiryService inquiryService) {
        this.inquiryService = inquiryService;
    }

    @PostMapping
    public ResponseEntity<InquiryResponse> createInquiry(@NonNull @Valid @RequestBody Inquiry inquiry) {
        InquiryResponse responseInquiry = inquiryService.createInquiry(inquiry);
        return new ResponseEntity<>(responseInquiry, HttpStatus.CREATED);
        
    }

    @PreAuthorize("@permissionService.hasAccess('MANAGE_INQUIRIES')")
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllInquiry(
        @RequestParam(value = "page", defaultValue = "1") int page,
        @RequestParam(value = "limit", defaultValue = "10") int limit,
        @RequestParam(value = "sortBy", required = false) String sortBy,
        @RequestParam(value = "sortDirection", required = false) String sortDirection
    ) {
        Map<String, Object> response = new HashMap<>();
        Page<InquiryResponse> result = inquiryService.getAllInquiry(page, limit, sortBy, sortDirection);
        
        response.put("data", result.getContent());
        response.put("total", result.getTotalElements());
        response.put("page", page);
        response.put("pages", result.getTotalPages());

        return ResponseEntity.ok(response);
        
    }

    @PreAuthorize("@permissionService.hasAccess('MANAGE_INQUIRIES')")
    @GetMapping("/{id}")
    public Inquiry getById(
        @PathVariable String id
    ) {
        if(id != null && !id.isBlank()) {
            return inquiryService.getById(id);
        }
        throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid ID");
    }

    @PreAuthorize("@permissionService.hasAccess('MANAGE_INQUIRIES')")
    @PutMapping("/{id}/status")
    public ResponseEntity<Inquiry> updateStatus(
        @PathVariable("id") String id,
        @RequestBody Map<String, String> payload
    ) {
        String status = payload.get("status");
        Inquiry updatedInquiry = inquiryService.updateStatus(id, status);
        return ResponseEntity.ok(updatedInquiry);
    }
}
