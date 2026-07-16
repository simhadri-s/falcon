package com.laserxprts.falcon.controllers;

import java.security.Principal;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.laserxprts.falcon.dto.request.CouponRequest;
import com.laserxprts.falcon.dto.request.CouponValidationRequest;
import com.laserxprts.falcon.dto.response.CouponValidationResponse;
import com.laserxprts.falcon.model.Coupon;
import com.laserxprts.falcon.service.CouponService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    // ── ADMIN: Create coupon ─────────────────────────────────
    @PreAuthorize("@permissionService.hasAccess('MANAGE_ORDERS')")
    @PostMapping
    public ResponseEntity<Coupon> createCoupon(@RequestBody CouponRequest req) {
        return ResponseEntity.ok(couponService.createCoupon(req));
    }

    // ── ADMIN: List all coupons ──────────────────────────────
    @PreAuthorize("@permissionService.hasAccess('MANAGE_ORDERS')")
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllCoupons(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(couponService.getAllCoupons(keyword, page, limit));
    }

    // ── ADMIN: Toggle active ─────────────────────────────────
    @PreAuthorize("@permissionService.hasAccess('MANAGE_ORDERS')")
    @PatchMapping("/{id}/toggle")
    public ResponseEntity<Coupon> toggleActive(@PathVariable String id) {
        return ResponseEntity.ok(couponService.toggleActive(id));
    }

    // ── ADMIN: Delete coupon ─────────────────────────────────
    @PreAuthorize("@permissionService.hasAccess('MANAGE_ORDERS')")
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteCoupon(@PathVariable String id) {
        couponService.deleteCoupon(id);
        return ResponseEntity.ok("Coupon deleted successfully.");
    }

    // ── USER: Validate coupon before placing order ───────────
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/validate")
    public ResponseEntity<CouponValidationResponse> validateCoupon(
            Principal principal,
            @RequestBody CouponValidationRequest req) {
        String userEmail = principal.getName();
        
        if (req.getItems() != null && !req.getItems().isEmpty()) {
            java.util.List<CouponService.CouponItem> serviceItems = req.getItems().stream()
                .map(i -> new CouponService.CouponItem(i.getProductId(), i.getCategoryId(), i.getPrice(), i.getQuantity()))
                .collect(java.util.stream.Collectors.toList());
            return ResponseEntity.ok(couponService.validateCoupon(req.getCode(), serviceItems, req.getOrderTotal(), userEmail));
        }
        
        return ResponseEntity.ok(couponService.validateCoupon(req.getCode(), req.getOrderTotal(), userEmail));
    }
}
