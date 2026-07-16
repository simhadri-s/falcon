package com.laserxprts.falcon.service;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.laserxprts.falcon.dto.request.AddressRequest;
import com.laserxprts.falcon.model.Address;
import com.laserxprts.falcon.model.User;
import com.laserxprts.falcon.repository.AddressRepository;
import com.laserxprts.falcon.repository.DeliveryLocationRepository;
import com.laserxprts.falcon.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
public class AddressServiceTest {

    @Mock private AddressRepository addressRepository;
    @Mock private UserRepository userRepository;
    @Mock private DeliveryLocationRepository locationRepository;

    @InjectMocks
    private AddressService addressService;

    private User testUser;
    private Address testAddress;
    private AddressRequest request;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId("user-1");
        testUser.setEmail("test@test.com");

        testAddress = new Address();
        testAddress.setId("ADDR-1");
        testAddress.setUserId("user-1");
        testAddress.setPincode("110001");

        request = new AddressRequest();
        request.setPincode("110001");
        request.setDefault(true);
    }

    @Test
    void testAddAddress_Success() {
        when(locationRepository.existsByPincode("110001")).thenReturn(true);
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(testUser));
        when(addressRepository.save(any(Address.class))).thenReturn(testAddress);

        Address result = addressService.addAddress(request, "test@test.com");

        assertNotNull(result);
        assertEquals("110001", result.getPincode());
        verify(addressRepository).save(any(Address.class));
    }

    @Test
    void testAddAddress_InvalidPincode_ThrowsException() {
        when(locationRepository.existsByPincode("110001")).thenReturn(false);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> addressService.addAddress(request, "test@test.com"));
        assertTrue(ex.getMessage().contains("Delivery not available to this pincode"));
        verify(addressRepository, never()).save(any(Address.class));
    }

    @Test
    void testUpdateAddress_SecurityCheck_OtherUser_ThrowsException() {
        when(locationRepository.existsByPincode("110001")).thenReturn(true);
        
        User otherUser = new User();
        otherUser.setId("user-2");
        when(userRepository.findByEmail("other@test.com")).thenReturn(Optional.of(otherUser));
        
        when(addressRepository.findById("ADDR-1")).thenReturn(Optional.of(testAddress)); // address belongs to user-1

        RuntimeException ex = assertThrows(RuntimeException.class, () -> addressService.updateAddress("ADDR-1", request, "other@test.com"));
        assertEquals("Unauthorized", ex.getMessage());
        verify(addressRepository, never()).save(any(Address.class));
    }
}
