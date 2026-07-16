package com.laserxprts.falcon.model;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {
    @Id
    private String id;

    @Version
    private Long version;

    @Indexed
    private String userId;
    private List<OrderItem> items;
    private AddressSnapshot addressSnapshot;
    @Indexed
    private String status;
    private double deliveryCharge;
    private String couponCode;
    private double discountAmount;
    private ReceiptDocument receipt;

    @Indexed
    @CreatedDate
    private LocalDateTime createdAt;

    @org.springframework.data.annotation.Transient
    private ReturnRequest returnRequest;
}
