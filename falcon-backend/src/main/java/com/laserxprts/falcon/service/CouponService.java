package com.laserxprts.falcon.service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.laserxprts.falcon.dto.request.CouponRequest;
import com.laserxprts.falcon.dto.response.CouponValidationResponse;
import com.laserxprts.falcon.exception.ApiException;
import com.laserxprts.falcon.model.Cart;
import com.laserxprts.falcon.model.CartItem;
import com.laserxprts.falcon.model.Coupon;
import com.laserxprts.falcon.repository.CartRepository;
import com.laserxprts.falcon.repository.CouponRepository;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CouponService {

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CouponItem {
        private String productId;
        private String categoryId;
        private double price;
        private int quantity;
    }

    private final CouponRepository couponRepository;
    private final CartRepository cartRepository;

    // ─────────────────────────────────────────────────────────
    // ADMIN: Create
    // ─────────────────────────────────────────────────────────

    public Coupon createCoupon(CouponRequest req) {
        String code = req.getCode().trim().toUpperCase();

        if (couponRepository.findByCodeIgnoreCase(code).isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT, "A coupon with this code already exists.");
        }

        if (!"FLAT".equalsIgnoreCase(req.getDiscountType()) && !"PERCENTAGE".equalsIgnoreCase(req.getDiscountType())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "discountType must be FLAT or PERCENTAGE.");
        }

        if (req.getDiscountValue() <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "discountValue must be greater than 0.");
        }

        if ("PERCENTAGE".equalsIgnoreCase(req.getDiscountType()) && req.getDiscountValue() > 100) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Percentage discount cannot exceed 100.");
        }

        Coupon coupon = Coupon.builder()
                .code(code)
                .discountType(req.getDiscountType().toUpperCase())
                .discountValue(req.getDiscountValue())
                .minOrderValue(req.getMinOrderValue())
                .expiryDate(req.getExpiryDate())
                .maxUses(req.getMaxUses())
                .applicableProductIds(req.getApplicableProductIds())
                .applicableCategoryIds(req.getApplicableCategoryIds())
                .isActive(req.isActive())
                .createdAt(LocalDateTime.now(ZoneId.of("Asia/Kolkata")))
                .build();

        return couponRepository.save(coupon);
    }

    // ─────────────────────────────────────────────────────────
    // ADMIN: List with pagination + search
    // ─────────────────────────────────────────────────────────

    public Map<String, Object> getAllCoupons(String keyword, int page, int limit) {
        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), limit, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Coupon> result = (keyword != null && !keyword.isBlank())
                ? couponRepository.searchByCode(keyword, pageable)
                : couponRepository.findAll(pageable);

        Map<String, Object> response = new HashMap<>();
        response.put("data", result.getContent());
        response.put("total", result.getTotalElements());
        response.put("page", page);
        response.put("pages", result.getTotalPages());
        return response;
    }

    // ─────────────────────────────────────────────────────────
    // ADMIN: Toggle active
    // ─────────────────────────────────────────────────────────

    public Coupon toggleActive(String id) {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Coupon not found."));
        coupon.setActive(!coupon.isActive());
        return couponRepository.save(coupon);
    }

    // ─────────────────────────────────────────────────────────
    // ADMIN: Delete
    // ─────────────────────────────────────────────────────────

    public void deleteCoupon(String id) {
        if (!couponRepository.existsById(id)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Coupon not found.");
        }
        couponRepository.deleteById(id);
    }

    // ─────────────────────────────────────────────────────────
    // USER: Validate a coupon before placing order
    // ─────────────────────────────────────────────────────────

    public CouponValidationResponse validateCoupon(String code, double orderTotal, String userEmail) {
        // Default: validate against current cart
        Cart cart = cartRepository.findByUserId(userEmail).orElse(null);
        if (cart == null || cart.getItems().isEmpty()) {
            return CouponValidationResponse.builder().valid(false).message("Your cart is empty.").build();
        }

        List<CouponItem> items = cart.getItems().stream()
            .filter(CartItem::isActive)
            .map(i -> new CouponItem(i.getProduct().getId(), i.getProduct().getCategoryId(), 
                (i.getProduct().getSellingPrice() != null && i.getProduct().getSellingPrice() > 0 ? i.getProduct().getSellingPrice() : i.getProduct().getMrp()), 
                i.getQuantity()))
            .collect(Collectors.toList());

        return validateCoupon(code, items, orderTotal, userEmail);
    }

    public CouponValidationResponse validateCoupon(String code, List<CouponItem> items, double orderTotal, String userEmail) {
        Coupon coupon = couponRepository.findByCodeIgnoreCase(code).orElse(null);

        if (coupon == null) {
            return CouponValidationResponse.builder().valid(false).message("Coupon code not found.").build();
        }

        if (!coupon.isActive()) {
            return CouponValidationResponse.builder().valid(false).message("This coupon is no longer active.").build();
        }

        if (coupon.getExpiryDate() != null && coupon.getExpiryDate().isBefore(LocalDateTime.now(ZoneId.of("Asia/Kolkata")))) {
            return CouponValidationResponse.builder().valid(false).message("This coupon has expired.").build();
        }

        if (coupon.getMaxUses() == 1 && coupon.getUsedCount() >= 1) {
            return CouponValidationResponse.builder().valid(false).message("This coupon has already been claimed.").build();
        }

        if (coupon.getMaxUses() > 1 && coupon.getUsedCount() >= coupon.getMaxUses()) {
            return CouponValidationResponse.builder().valid(false).message("This coupon has reached its maximum number of uses.").build();
        }

        if (coupon.getMaxUses() != 1 && coupon.getUsedByUserEmails().contains(userEmail)) {
            return CouponValidationResponse.builder().valid(false).message("You have already used this coupon.").build();
        }

        // --- Restriction Checks ---
        if (items == null || items.isEmpty()) {
            return CouponValidationResponse.builder().valid(false).message("No items found for validation.").build();
        }

        double applicableSubtotal = 0;
        boolean hasRestrictions = (coupon.getApplicableProductIds() != null && !coupon.getApplicableProductIds().isEmpty())
                || (coupon.getApplicableCategoryIds() != null && !coupon.getApplicableCategoryIds().isEmpty());

        if (hasRestrictions) {
            for (CouponItem item : items) {
                boolean productMatches = coupon.getApplicableProductIds() != null && coupon.getApplicableProductIds().contains(item.getProductId());
                boolean categoryMatches = coupon.getApplicableCategoryIds() != null && coupon.getApplicableCategoryIds().contains(item.getCategoryId());

                if (productMatches || categoryMatches) {
                    applicableSubtotal += item.getPrice() * item.getQuantity();
                }
            }

            if (applicableSubtotal <= 0) {
                return CouponValidationResponse.builder()
                        .valid(false)
                        .message("This coupon is not applicable to the selected items.")
                        .build();
            }
        } else {
            applicableSubtotal = orderTotal;
        }

        // Minimum order value check (on applicable total)
        if (coupon.getMinOrderValue() > 0 && applicableSubtotal < coupon.getMinOrderValue()) {
            return CouponValidationResponse.builder()
                    .valid(false)
                    .message(String.format("Minimum applicable value of ₹%.0f is required for this coupon.", coupon.getMinOrderValue()))
                    .build();
        }

        double discount = calculateDiscount(coupon, applicableSubtotal);

        return CouponValidationResponse.builder()
                .valid(true)
                .discountAmount(discount)
                .couponCode(coupon.getCode())
                .message("Coupon applied successfully!")
                .build();
    }

    // ─────────────────────────────────────────────────────────
    // INTERNAL: Mark coupon as used after order is saved
    // ─────────────────────────────────────────────────────────

    public void markCouponUsed(String code, String userEmail) {
        couponRepository.findByCodeIgnoreCase(code).ifPresent(coupon -> {
            coupon.setUsedCount(coupon.getUsedCount() + 1);
            if (!coupon.getUsedByUserEmails().contains(userEmail)) {
                coupon.getUsedByUserEmails().add(userEmail);
            }
            couponRepository.save(coupon);
        });
    }

    // ─────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────

    private double calculateDiscount(Coupon coupon, double orderTotal) {
        if ("PERCENTAGE".equals(coupon.getDiscountType())) {
            return Math.round((orderTotal * coupon.getDiscountValue() / 100.0) * 100.0) / 100.0;
        }
        // FLAT — cannot exceed order total
        return Math.min(coupon.getDiscountValue(), orderTotal);
    }
}
