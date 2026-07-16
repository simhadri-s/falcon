package com.laserxprts.falcon.dto.request;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OrderRequest {

  private List<OrderItemRequest> items;
  private String addressId;
  private String couponCode;

}