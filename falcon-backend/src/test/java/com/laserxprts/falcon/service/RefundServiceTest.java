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

import com.laserxprts.falcon.enums.RefundMethod;
import com.laserxprts.falcon.enums.RefundStatus;
import com.laserxprts.falcon.model.Refund;
import com.laserxprts.falcon.repository.RefundRepository;

@ExtendWith(MockitoExtension.class)
public class RefundServiceTest {

    @Mock private RefundRepository refundRepository;

    @InjectMocks
    private RefundService refundService;

    private Refund testRefund;

    @BeforeEach
    void setUp() {
        testRefund = Refund.builder()
            .id("REF-1")
            .orderId("ORD-1")
            .userId("user-1")
            .amount(100.0)
            .method(RefundMethod.ORIGINAL_PAYMENT)
            .status(RefundStatus.REFUND_PENDING)
            .build();
    }

    @Test
    void testCreateRefund() {
        when(refundRepository.save(any(Refund.class))).thenReturn(testRefund);

        Refund result = refundService.createRefund("ORD-1", "user-1", "RET-1", 100.0, RefundMethod.ORIGINAL_PAYMENT);

        assertNotNull(result);
        assertEquals(RefundStatus.REFUND_PENDING, result.getStatus());
        assertEquals(100.0, result.getAmount());
        verify(refundRepository).save(any(Refund.class));
    }

    @Test
    void testUpdateRefundStatus() {
        when(refundRepository.findById("REF-1")).thenReturn(Optional.of(testRefund));
        when(refundRepository.save(any(Refund.class))).thenReturn(testRefund);

        Refund result = refundService.updateRefundStatus("REF-1", RefundStatus.REFUND_COMPLETED, "TXN-123", "Done");

        assertNotNull(result);
        assertEquals(RefundStatus.REFUND_COMPLETED, testRefund.getStatus());
        assertEquals("TXN-123", testRefund.getTransactionId());
        assertEquals("Done", testRefund.getNotes());
        verify(refundRepository).save(testRefund);
    }
}
