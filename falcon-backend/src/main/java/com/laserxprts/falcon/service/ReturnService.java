package com.laserxprts.falcon.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.laserxprts.falcon.dto.request.ReturnCreateRequest;
import com.laserxprts.falcon.enums.OrderStatus;
import com.laserxprts.falcon.enums.RefundMethod;
import com.laserxprts.falcon.enums.ReturnStatus;
import com.laserxprts.falcon.exception.ApiException;
import com.laserxprts.falcon.model.CompanySettings;
import com.laserxprts.falcon.model.Order;
import com.laserxprts.falcon.model.OrderItem;
import com.laserxprts.falcon.model.ReturnRequest;
import com.laserxprts.falcon.repository.CompanySettingsRepository;
import com.laserxprts.falcon.repository.OrderRepository;
import com.laserxprts.falcon.repository.ReturnRequestRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReturnService {

    private final ReturnRequestRepository returnRequestRepository;
    private final OrderRepository orderRepository;
    private final CompanySettingsRepository companySettingsRepository;
    private final RefundService refundService;

    public ReturnRequest createReturnRequest(String userId, ReturnCreateRequest request) {
        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Order not found"));

        if (!order.getUserId().equals(userId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You are not authorized to return this order");
        }

        if (!order.getStatus().equals(OrderStatus.DELIVERED.name())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Only delivered orders can be returned");
        }

        // Check return window
        CompanySettings settings = companySettingsRepository.findById("COMPANY_SETTINGS")
                .orElse(null);
        int returnWindow = (settings != null && settings.getReturnWindowDays() != null) ? settings.getReturnWindowDays() : 7;
        
        if (order.getCreatedAt().plusDays(returnWindow).isBefore(LocalDateTime.now())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Return window has expired");
        }

        // Check if return already exists for this order
        // For simplicity, we prevent multiple active returns. 
        // In a more advanced system, we would check which specific items are being returned.
        boolean hasActiveReturn = returnRequestRepository.findByOrderId(order.getId()).stream()
                .anyMatch(r -> r.getStatus() != ReturnStatus.RETURN_REJECTED);
        
        if (hasActiveReturn) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "A return request already exists for this order");
        }

        List<ReturnRequest.ReturnItem> returnItems = new ArrayList<>();
        for (ReturnCreateRequest.ReturnItemRequest itemReq : request.getItems()) {
            OrderItem orderItem = order.getItems().stream()
                    .filter(i -> i.getId().equals(itemReq.getOrderItemId()))
                    .findFirst()
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Order item not found: " + itemReq.getOrderItemId()));
            
            if (itemReq.getQuantity() > orderItem.getQuantity()) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Return quantity exceeds ordered quantity for item: " + orderItem.getProductSnapshot().getName());
            }

            returnItems.add(ReturnRequest.ReturnItem.builder()
                    .orderItemId(orderItem.getId())
                    .quantity(itemReq.getQuantity())
                    .productSnapshot(orderItem.getProductSnapshot())
                    .build());
        }

        ReturnRequest returnRequest = ReturnRequest.builder()
                .orderId(order.getId())
                .userId(userId)
                .items(returnItems)
                .reason(request.getReason())
                .comment(request.getComment())
                .status(ReturnStatus.RETURN_REQUESTED)
                .createdAt(LocalDateTime.now())
                .build();

        return returnRequestRepository.save(returnRequest);
    }

    public ReturnRequest updateReturnStatus(String returnId, ReturnStatus status, String adminComment) {
        ReturnRequest returnRequest = returnRequestRepository.findById(returnId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Return request not found"));

        ReturnStatus oldStatus = returnRequest.getStatus();
        returnRequest.setStatus(status);
        if (adminComment != null) returnRequest.setAdminComment(adminComment);
        returnRequest.setUpdatedAt(LocalDateTime.now());

        ReturnRequest savedRequest = returnRequestRepository.save(returnRequest);

        // If approved or completed, initiate refund if not already done
        if (status == ReturnStatus.RETURN_APPROVED && oldStatus == ReturnStatus.RETURN_REQUESTED) {
            calculateAndInitiateRefund(savedRequest);
        }

        return savedRequest;
    }

    private void calculateAndInitiateRefund(ReturnRequest returnRequest) {
        Order order = orderRepository.findById(returnRequest.getOrderId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Order not found"));

        double refundAmount = 0.0;
        for (ReturnRequest.ReturnItem returnItem : returnRequest.getItems()) {
            Double price = returnItem.getProductSnapshot().getSellingPrice();
            if (price == null) price = returnItem.getProductSnapshot().getMrp();
            refundAmount += (price != null ? price : 0.0) * returnItem.getQuantity();
        }

        // Pro-rata discount calculation if needed, but let's keep it simple for now.
        // If entire order is returned, refund discount too? 
        // Usually, we refund what the user paid.
        
        // Simple logic: If all items are returned, refund (total items - discount + delivery charge?)
        // For partial, we just refund the item prices.
        
        refundService.createRefund(
                order.getId(),
                order.getUserId(),
                returnRequest.getId(),
                refundAmount,
                RefundMethod.ORIGINAL_PAYMENT // Default
        );
    }

    public List<ReturnRequest> getReturnsByUserId(String userId) {
        return returnRequestRepository.findByUserId(userId);
    }

    public List<ReturnRequest> getAllReturns() {
        return returnRequestRepository.findAll();
    }

    public ReturnRequest getReturnById(String id) {
        return returnRequestRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Return request not found"));
    }
}
