package com.laserxprts.falcon.controllers;

import java.security.Principal;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.laserxprts.falcon.enums.RefundStatus;
import com.laserxprts.falcon.model.Refund;
import com.laserxprts.falcon.service.RefundService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/refunds")
@RequiredArgsConstructor
public class RefundController {

    private final RefundService refundService;

    @GetMapping("/my")
    public ResponseEntity<List<Refund>> getMyRefunds(Principal principal) {
        return ResponseEntity.ok(refundService.getRefundsByUserId(principal.getName()));
    }

    // Admin Endpoints
    @GetMapping
    public ResponseEntity<List<Refund>> getAllRefunds() {
        return ResponseEntity.ok(refundService.getAllRefunds());
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Refund> updateRefundStatus(
            @PathVariable String id,
            @RequestParam RefundStatus status,
            @RequestParam(required = false) String transactionId,
            @RequestParam(required = false) String notes) {
        return ResponseEntity.ok(refundService.updateRefundStatus(id, status, transactionId, notes));
    }
}
