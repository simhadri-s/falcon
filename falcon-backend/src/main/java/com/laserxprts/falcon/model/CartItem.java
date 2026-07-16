package com.laserxprts.falcon.model;

import java.util.UUID;

import org.springframework.data.mongodb.core.mapping.DocumentReference;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CartItem {
    private String id = UUID.randomUUID().toString();

    @DocumentReference(lazy = true)
    private Product product;

    private String variantId;

    private int quantity;
    private boolean active = true;
}
