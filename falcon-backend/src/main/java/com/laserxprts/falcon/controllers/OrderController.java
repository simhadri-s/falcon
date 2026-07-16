package com.laserxprts.falcon.controllers;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.laserxprts.falcon.dto.request.OrderRequest;
import com.laserxprts.falcon.model.Order;
import com.laserxprts.falcon.security.PermissionService;
import com.laserxprts.falcon.service.OrderReceiptService;
import com.laserxprts.falcon.service.OrderService;


import lombok.RequiredArgsConstructor;

@PreAuthorize("isAuthenticated()")
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;
    private final PermissionService permissionService;

    @PostMapping
    public Order createOrder(Principal principal, @RequestBody OrderRequest orderRequest) {
        String userEmail = principal.getName();
        return orderService.createOrder(userEmail, orderRequest);
    }

    @PutMapping("/{orderId}/address")
    public ResponseEntity<String> updateOrderAddress(
        Principal principal,
        @PathVariable String orderId,
        @RequestParam(value = "addressId", required = false) String addressId
    ) {
        orderService.updateOrderAddress(principal.getName(), orderId, addressId);
        return ResponseEntity.ok("Address Updated Successfully");
    }
    
    @PutMapping("/{orderId}/cancel")
    public ResponseEntity<String> cancelOrder(@PathVariable String orderId) {
        orderService.cancelOrder(orderId);
        return ResponseEntity.ok("Order cancelled successfully");
    }

    @PreAuthorize("@permissionService.hasAccess('MANAGE_ORDERS')")
    @PutMapping("/{orderId}/status")
    public ResponseEntity<Order> updateOrderStatus(@PathVariable String orderId, @RequestBody Map<String, String> request) {
        Order updatedOrder = orderService.updateOrderStatus(orderId, request.get("orderStatus"));
        return ResponseEntity.ok(updatedOrder);
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllOrders(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(value = "sortBy", required = false) String sortBy,
            @RequestParam(value = "sortDirection", required = false) String sortDirection,
            Principal principal) {
                
        String userId = principal.getName();

        boolean isAdmin = permissionService.hasAccess("MANAGE_ORDERS");

        // 3. Pass everything to the service layer
        Page<Order> orders = orderService.getAllOrders(keyword, status, page, limit, sortBy, sortDirection, userId, isAdmin);

        // 4. Format the response
        Map<String, Object> response = new HashMap<>();
        response.put("data", orders.getContent());
        response.put("total", orders.getTotalElements());
        response.put("page", page);
        response.put("pages", orders.getTotalPages());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<Order> getOrderById(Principal principal, @PathVariable String orderId) {
        boolean isAdmin = permissionService.hasAccess("MANAGE_ORDERS");
        return ResponseEntity.ok(orderService.getOrderById(principal.getName(), orderId, isAdmin));
    }

    @GetMapping("/{orderId}/receipt")
    public ResponseEntity<ByteArrayResource> downloadReceipt(Principal principal, @PathVariable String orderId) {
        boolean isAdmin = permissionService.hasAccess("MANAGE_ORDERS");
        OrderReceiptService.ReceiptDownload receiptDownload = orderService.downloadReceipt(principal.getName(), orderId, isAdmin);

        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_PDF)
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + receiptDownload.fileName() + "\"")
            .body(new ByteArrayResource(receiptDownload.content()));
    }


    @PreAuthorize("@permissionService.hasAccess('MANAGE_ORDERS')")
    @PostMapping("/status-count")
    public ResponseEntity<Map<String, Long>> getCountByStatus(@RequestBody Map<String, String> request) {
        long count = orderService.getCountByStatus(request.get("statusType").toUpperCase());
        return ResponseEntity.ok(Map.of("count", count));
    } 
    
}
