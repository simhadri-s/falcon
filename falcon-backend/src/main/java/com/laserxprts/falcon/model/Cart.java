package com.laserxprts.falcon.model;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Document(collection = "carts")
public class Cart {
    @Id
    private String id;

    @Version
    private Long version;

    @Indexed
    private String userId;

    private List<CartItem> items = new ArrayList<>();
}
