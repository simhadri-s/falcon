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

import com.laserxprts.falcon.dto.request.PriceRequest;
import com.laserxprts.falcon.model.Price;
import com.laserxprts.falcon.repository.PriceRepository;

@ExtendWith(MockitoExtension.class)
public class PriceServiceTest {

    @Mock private PriceRepository priceRepository;

    @InjectMocks
    private PriceService priceService;

    private Price testPrice;

    @BeforeEach
    void setUp() {
        testPrice = new Price();
        testPrice.setId("PRICE-1");
        testPrice.setPrice(99.99);
        testPrice.setCurrency("USD");
    }

    @Test
    void testCreatePrice() {
        when(priceRepository.save(any(Price.class))).thenReturn(testPrice);

        PriceRequest req = new PriceRequest();
        req.setAmount(99.99);
        req.setCurrency("USD");

        PriceRequest result = priceService.createPrice(req);

        assertNotNull(result);
        assertEquals(99.99, result.getAmount());
        assertEquals("USD", result.getCurrency());
        verify(priceRepository).save(any(Price.class));
    }

    @Test
    void testUpdatePrice() {
        when(priceRepository.findById("PRICE-1")).thenReturn(Optional.of(testPrice));
        when(priceRepository.save(any(Price.class))).thenReturn(testPrice);

        PriceRequest req = new PriceRequest();
        req.setAmount(89.99);
        req.setCurrency("EUR");

        PriceRequest result = priceService.updatePrice("PRICE-1", req);

        assertNotNull(result);
        verify(priceRepository).save(any(Price.class));
    }

    @Test
    void testDeletePrice() {
        when(priceRepository.existsById("PRICE-1")).thenReturn(true);

        boolean result = priceService.deletePrice("PRICE-1");

        assertTrue(result);
        verify(priceRepository).deleteById("PRICE-1");
    }
}
