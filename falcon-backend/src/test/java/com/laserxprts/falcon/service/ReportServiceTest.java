package com.laserxprts.falcon.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.laserxprts.falcon.dto.response.ReportDashboardResponse;
import com.laserxprts.falcon.repository.OrderRepository;
import com.laserxprts.falcon.repository.ProductRepository;
import com.laserxprts.falcon.repository.RoleRepository;
import com.laserxprts.falcon.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
public class ReportServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private ProductRepository productRepository;
    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;

    @InjectMocks
    private ReportService reportService;

    @Test
    void testGetDashboardReport() {
        when(productRepository.findAll(org.mockito.ArgumentMatchers.any(org.springframework.data.domain.Sort.class))).thenReturn(Collections.emptyList());

        ReportDashboardResponse result = reportService.getDashboardReport("ALL_TIME");

        assertNotNull(result);
        assertEquals(0, result.overview().totalOrders());
        assertEquals(0.0, result.overview().totalRevenue());
        assertEquals(0, result.overview().totalCustomers());
    }
}
