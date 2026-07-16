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
import org.springframework.http.HttpStatus;

import com.laserxprts.falcon.dto.request.CouponRequest;
import com.laserxprts.falcon.exception.ApiException;
import com.laserxprts.falcon.model.Coupon;
import com.laserxprts.falcon.repository.CartRepository;
import com.laserxprts.falcon.repository.CouponRepository;

@ExtendWith(MockitoExtension.class)
public class CouponServiceTest {

    @Mock private CouponRepository couponRepository;
    @Mock private CartRepository cartRepository;

    @InjectMocks
    private CouponService couponService;

    private Coupon coupon;
    private CouponRequest request;

    @BeforeEach
    void setUp() {
        coupon = new Coupon();
        coupon.setId("C1");
        coupon.setCode("SUMMER50");
        coupon.setDiscountType("FLAT");
        coupon.setDiscountValue(50);
        coupon.setMaxUses(1);
        coupon.setUsedCount(0);
        coupon.setVersion(1L);

        request = new CouponRequest();
        request.setCode("SUMMER50");
        request.setDiscountType("FLAT");
        request.setDiscountValue(50);
    }

    @Test
    void testCreateCoupon_Success() {
        when(couponRepository.findByCodeIgnoreCase("SUMMER50")).thenReturn(Optional.empty());
        when(couponRepository.save(any(Coupon.class))).thenReturn(coupon);

        Coupon created = couponService.createCoupon(request);

        assertNotNull(created);
        assertEquals("SUMMER50", created.getCode());
        verify(couponRepository).save(any(Coupon.class));
    }

    @Test
    void testCreateCoupon_DuplicateCode_ThrowsException() {
        when(couponRepository.findByCodeIgnoreCase("SUMMER50")).thenReturn(Optional.of(coupon));

        ApiException ex = assertThrows(ApiException.class, () -> couponService.createCoupon(request));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
    }

    @Test
    void testCreateCoupon_InvalidPercentage_ThrowsException() {
        request.setDiscountType("PERCENTAGE");
        request.setDiscountValue(150); // > 100

        when(couponRepository.findByCodeIgnoreCase("SUMMER50")).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class, () -> couponService.createCoupon(request));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }
}
