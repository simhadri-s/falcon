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
import org.springframework.http.HttpStatus;

import com.laserxprts.falcon.exception.ApiException;
import com.laserxprts.falcon.model.Order;
import com.laserxprts.falcon.repository.OrderRepository;

@ExtendWith(MockitoExtension.class)
public class OrderServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private EmailService emailService;
    @Mock private WhatsappService whatsappService;
    @Mock private com.laserxprts.falcon.repository.UserRepository userRepository;
    @Mock private FcmService fcmService;
    @Mock private com.laserxprts.falcon.repository.ReturnRequestRepository returnRequestRepository;

    @InjectMocks
    private OrderService orderService;

    private Order testOrder;

    @BeforeEach
    void setUp() {
        testOrder = new Order();
        testOrder.setId("ORD123");
        testOrder.setUserId("user-1");
        testOrder.setStatus("CREATED");
        
        lenient().when(returnRequestRepository.findByOrderId(anyString())).thenReturn(new java.util.ArrayList<>());
    }

    @Test
    void testUpdateOrderStatus_Success() {
        when(orderRepository.findById("ORD123")).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        Order updatedOrder = orderService.updateOrderStatus("ORD123", "SHIPPED");

        assertEquals("SHIPPED", updatedOrder.getStatus());
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void testCancelOrder_Success() {
        when(orderRepository.findById("ORD123")).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        orderService.updateOrderStatus("ORD123", "CANCELLED");

        assertEquals("CANCELLED", testOrder.getStatus());
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void testUpdateOrderStatus_InvalidStatus_ThrowsException() {
        when(orderRepository.findById("ORD123")).thenReturn(Optional.of(testOrder));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> orderService.updateOrderStatus("ORD123", "INVALID_STATUS"));
        assertTrue(ex.getMessage().contains("Invalid order status"));
    }
}
