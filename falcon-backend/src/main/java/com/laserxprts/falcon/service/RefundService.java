package com.laserxprts.falcon.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.laserxprts.falcon.enums.RefundMethod;
import com.laserxprts.falcon.enums.RefundStatus;
import com.laserxprts.falcon.model.Refund;
import com.laserxprts.falcon.repository.RefundRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RefundService {

    private final RefundRepository refundRepository;

    public Refund createRefund(String orderId, String userId, String returnRequestId, double amount, RefundMethod method) {
        Refund refund = Refund.builder()
                .orderId(orderId)
                .userId(userId)
                .returnRequestId(returnRequestId)
                .amount(amount)
                .method(method)
                .status(RefundStatus.REFUND_PENDING)
                .createdAt(LocalDateTime.now())
                .build();
        return refundRepository.save(refund);
    }

    public Refund updateRefundStatus(String refundId, RefundStatus status, String transactionId, String notes) {
        Refund refund = refundRepository.findById(refundId)
                .orElseThrow(() -> new RuntimeException("Refund not found"));
        refund.setStatus(status);
        if (transactionId != null) refund.setTransactionId(transactionId);
        if (notes != null) refund.setNotes(notes);
        refund.setUpdatedAt(LocalDateTime.now());
        return refundRepository.save(refund);
    }

    public List<Refund> getRefundsByUserId(String userId) {
        return refundRepository.findByUserId(userId);
    }

    public List<Refund> getAllRefunds() {
        return refundRepository.findAll();
    }
    
    public List<Refund> getRefundsByOrderId(String orderId) {
        return refundRepository.findByOrderId(orderId);
    }
}
