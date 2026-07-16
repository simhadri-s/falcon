package com.laserxprts.falcon.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.laserxprts.falcon.dto.request.ReturnCreateRequest;
import com.laserxprts.falcon.enums.OrderStatus;
import com.laserxprts.falcon.exception.ApiException;
import com.laserxprts.falcon.model.Order;
import com.laserxprts.falcon.repository.CompanySettingsRepository;
import com.laserxprts.falcon.repository.OrderRepository;
import com.laserxprts.falcon.repository.ReturnRequestRepository;

@ExtendWith(MockitoExtension.class)
public class ReturnServiceTest {

    @Mock private ReturnRequestRepository returnRequestRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private CompanySettingsRepository companySettingsRepository;
    @Mock private RefundService refundService;

    @InjectMocks
    private ReturnService returnService;

    private Order testOrder;
    private ReturnCreateRequest request;

    @BeforeEach
    void setUp() {
        testOrder = new Order();
        testOrder.setId("ORD-1");
        testOrder.setUserId("user-1");
        testOrder.setStatus(OrderStatus.DELIVERED.name());
        testOrder.setCreatedAt(LocalDateTime.now().minusDays(2));

        request = new ReturnCreateRequest();
        request.setOrderId("ORD-1");
        request.setReason("Defective");
    }

    @Test
    void testCreateReturnRequest_Unauthorized_ThrowsException() {
        when(orderRepository.findById("ORD-1")).thenReturn(Optional.of(testOrder));

        ApiException ex = assertThrows(ApiException.class, () -> returnService.createReturnRequest("other-user", request));
        assertEquals("You are not authorized to return this order", ex.getMessage());
    }

    @Test
    void testCreateReturnRequest_NotDelivered_ThrowsException() {
        testOrder.setStatus(OrderStatus.CREATED.name());
        when(orderRepository.findById("ORD-1")).thenReturn(Optional.of(testOrder));

        ApiException ex = assertThrows(ApiException.class, () -> returnService.createReturnRequest("user-1", request));
        assertEquals("Only delivered orders can be returned", ex.getMessage());
    }
}
