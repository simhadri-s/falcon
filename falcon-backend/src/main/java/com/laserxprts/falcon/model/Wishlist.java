package com.laserxprts.falcon.model;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.DocumentReference;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Document(collection = "wishlists")
public class Wishlist {
    @Id
    private String id;

    @Version
    private Long version;

    private String userId;

    @DocumentReference(lazy = true)
    private List<Product> products = new ArrayList<>();
}
