package com.laserxprts.falcon.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.annotation.Version;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "coupons")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Coupon {

    @Id
    private String id;

    @Version
    private Long version;

    @Indexed(unique = true)
    private String code;

    /** FLAT or PERCENTAGE */
    private String discountType;

    private double discountValue;

    /** Minimum order value required to use this coupon (0 = no minimum) */
    private double minOrderValue;

    /** Expiry date/time; null = no expiry */
    private LocalDateTime expiryDate;

    /**
     * Maximum number of total redemptions allowed.
     * 1  = single-use (only one person ever can claim it)
     * 0  = unlimited (each user can still only claim it once)
     * N  = N total redemptions allowed (each user can still only claim it once)
     */
    private int maxUses;

    /** Running count of how many times this coupon has been redeemed */
    @Builder.Default
    private int usedCount = 0;

    /**
     * List of user emails that have already redeemed this coupon.
     * Used to enforce the "each user can only claim once" rule for multi-use coupons.
     */
    @Builder.Default
    private List<String> usedByUserEmails = new ArrayList<>();

    private List<String> applicableProductIds;
    private List<String> applicableCategoryIds;

    private boolean isActive;

    @CreatedDate
    private LocalDateTime createdAt;
}
