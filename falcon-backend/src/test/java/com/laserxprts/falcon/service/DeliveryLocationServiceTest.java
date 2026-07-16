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

import com.laserxprts.falcon.model.DeliveryLocation;
import com.laserxprts.falcon.repository.DeliveryLocationRepository;

@ExtendWith(MockitoExtension.class)
public class DeliveryLocationServiceTest {

    @Mock private DeliveryLocationRepository locationRepository;

    @InjectMocks
    private DeliveryLocationService deliveryLocationService;

    private DeliveryLocation testLocation;

    @BeforeEach
    void setUp() {
        testLocation = new DeliveryLocation();
        testLocation.setId("LOC-1");
        testLocation.setPincode("110001");
        testLocation.setLocation("New Delhi");
    }

    @Test
    void testCreateLocation_Success() {
        when(locationRepository.save(any(DeliveryLocation.class))).thenReturn(testLocation);

        DeliveryLocation location = new DeliveryLocation();
        location.setPincode("110001");
        location.setLocation("New Delhi");

        DeliveryLocation result = deliveryLocationService.createLocation(location);

        assertNotNull(result);
        assertEquals("110001", result.getPincode());
        verify(locationRepository).save(any(DeliveryLocation.class));
    }

    @Test
    void testCreateLocation_NullPincode_ThrowsException() {
        DeliveryLocation location = new DeliveryLocation();
        location.setLocation("New Delhi");

        RuntimeException ex = assertThrows(RuntimeException.class, () -> deliveryLocationService.createLocation(location));
        assertEquals("Pincode is empty", ex.getMessage());
        verify(locationRepository, never()).save(any(DeliveryLocation.class));
    }
}
