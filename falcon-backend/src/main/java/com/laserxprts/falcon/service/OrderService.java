package com.laserxprts.falcon.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.laserxprts.falcon.dto.request.OrderRequest;
import com.laserxprts.falcon.dto.response.CouponValidationResponse;
import com.laserxprts.falcon.exception.ApiException;
import org.springframework.http.HttpStatus;
import com.laserxprts.falcon.model.Address;
import com.laserxprts.falcon.model.AddressSnapshot;
import com.laserxprts.falcon.model.Order;
import com.laserxprts.falcon.model.OrderItem;
import com.laserxprts.falcon.model.Product;
import com.laserxprts.falcon.model.ProductSnapshot;
import com.laserxprts.falcon.model.User;
import com.laserxprts.falcon.repository.AddressRepository;
import com.laserxprts.falcon.repository.OrderRepository;
import com.laserxprts.falcon.repository.ProductRepository;
import com.laserxprts.falcon.repository.UserRepository;
import com.laserxprts.falcon.model.DeliveryLocation;
import com.laserxprts.falcon.repository.DeliveryLocationRepository;
import com.laserxprts.falcon.enums.OrderStatus;
import com.laserxprts.falcon.model.ReturnRequest;
import com.laserxprts.falcon.repository.ReturnRequestRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final AddressRepository addressRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final FcmService fcmService;
    private final DeliveryLocationRepository deliveryLocationRepository;
    private final CouponService couponService;
    private final com.laserxprts.falcon.repository.CompanySettingsRepository companySettingsRepository;
    private final ReturnRequestRepository returnRequestRepository;
    private final OrderReceiptService orderReceiptService;
    private final WhatsappService whatsappService;

    private String generateOrderId() {
        String prefix = companySettingsRepository.findById("COMPANY_SETTINGS")
                .map(com.laserxprts.falcon.model.CompanySettings::getOrderIdPrefix)
                .filter(p -> p != null && !p.trim().isEmpty())
                .orElse("ORD");

        int randomLength = 10 - prefix.length();
        if (randomLength < 1)
            randomLength = 4; // Fallback if prefix is too long

        java.util.Random random = new java.util.Random();
        StringBuilder sb = new StringBuilder(prefix);
        for (int i = 0; i < randomLength; i++) {
            sb.append(random.nextInt(10));
        }

        String orderId = sb.toString();

        // Check for collision against the _id field
        while (orderRepository.existsById(orderId)) {
            sb = new StringBuilder(prefix);
            for (int i = 0; i < randomLength; i++) {
                sb.append(random.nextInt(10));
            }
            orderId = sb.toString();
        }

        return orderId;
    }

    public Order createOrder(String userEmail, OrderRequest orderRequest) {

        if (orderRequest == null || orderRequest.getItems() == null) {
            throw new RuntimeException("Order is empty.");
        }

        List<String> productIds = orderRequest.getItems().stream()
                .map(i -> i.getProductId())
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());

        if (productIds.isEmpty() || productIds.size() != orderRequest.getItems().size()) {
            throw new RuntimeException("Product id can not be null");
        }

        List<Product> products = productRepository.findAllById(productIds);
        java.util.Map<String, Product> productMap = products.stream()
                .collect(Collectors.toMap(Product::getId, p -> p));

        List<Product> productsToUpdate = new java.util.ArrayList<>();

        List<OrderItem> items = orderRequest.getItems().stream()
                .map(i -> {
                    String productId = i.getProductId();
                    Product product = productMap.get(productId);
                    if (product == null) {
                        throw new RuntimeException("Product not found");
                    }

                    Product.ProductVariant variant = null;
                    if (product.isHasVariants()) {
                        if (i.getVariantId() == null || i.getVariantId().isBlank()) {
                            throw new ApiException(HttpStatus.BAD_REQUEST, "Please select a product variant for " + product.getName());
                        }
                        variant = product.getVariants().stream()
                            .filter(v -> v.getId().equals(i.getVariantId()))
                            .findFirst()
                            .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Selected variant not found for " + product.getName()));
                        
                        if (product.isManageStock()) {
                            if (variant.getStockQuantity() == null || variant.getStockQuantity() < i.getQuantity()) {
                                int available = variant.getStockQuantity() == null ? 0 : variant.getStockQuantity();
                                throw new ApiException(HttpStatus.BAD_REQUEST,
                                        "Insufficient stock for variant of " + product.getName() + " (Requested: " + i.getQuantity()
                                                + ", Available: " + available + ")");
                            }
                            variant.setStockQuantity(variant.getStockQuantity() - i.getQuantity());
                            productsToUpdate.add(product);
                        }
                    } else {
                        if (product.isManageStock()) {
                            if (product.getStockQuantity() == null || product.getStockQuantity() < i.getQuantity()) {
                                int available = product.getStockQuantity() == null ? 0 : product.getStockQuantity();
                                throw new ApiException(HttpStatus.BAD_REQUEST,
                                        "Insufficient stock for " + product.getName() + " (Requested: " + i.getQuantity()
                                                + ", Available: " + available + ")");
                            }
                            product.setStockQuantity(product.getStockQuantity() - i.getQuantity());
                            productsToUpdate.add(product);
                        }
                    }

                    ProductSnapshot snapshot = ProductSnapshot.from(product);
                    java.util.Map<String, String> variantAttributes = null;
                    if (variant != null) {
                        snapshot.setVariantId(variant.getId());
                        snapshot.setVariantAttributes(variant.getAttributes());
                        snapshot.setSellingPrice(variant.getSellingPrice() != null ? variant.getSellingPrice() : product.getSellingPrice());
                        snapshot.setMrp(variant.getMrp() != null ? variant.getMrp() : product.getMrp());
                        variantAttributes = variant.getAttributes();
                    }

                    return OrderItem.builder()
                            .productSnapshot(snapshot)
                            .variantId(i.getVariantId())
                            .variantAttributes(variantAttributes)
                            .quantity(i.getQuantity())
                            .build();
                })
                .collect(Collectors.toList());

        if (!productsToUpdate.isEmpty()) {
            productRepository.saveAll(productsToUpdate);
        }

        Address address;
        String requestAddressId = orderRequest.getAddressId();
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Failed to fetch user details"));
        if (requestAddressId != null && !requestAddressId.isBlank()) {
            address = addressRepository.findByIdAndUserId(requestAddressId, user.getId())
                    .orElseThrow(() -> new RuntimeException("Cannot find the address"));
        } else {
            address = addressRepository.findByUserIdAndIsDefaultTrue(user.getId())
                    .orElseThrow(() -> new RuntimeException(
                            "User address is not provided and default address is not present"));
        }
        AddressSnapshot addressSnapshot = AddressSnapshot.from(address);

        double deliveryCharge = 0.0;
        if (address.getPincode() != null && !address.getPincode().isBlank()) {
            deliveryCharge = deliveryLocationRepository.findByPincode(address.getPincode().trim())
                    .map(DeliveryLocation::getDeliveryCharge)
                    .orElse(0.0f);
        }

        // ── Coupon validation ────────────────────────────────
        double discountAmount = 0.0;
        String appliedCouponCode = null;
        String rawCouponCode = orderRequest.getCouponCode();
        if (rawCouponCode != null && !rawCouponCode.isBlank()) {
            double itemsTotal = items.stream()
                    .mapToDouble(i -> {
                        Double price = i.getProductSnapshot().getSellingPrice();
                        if (price == null)
                            price = i.getProductSnapshot().getMrp();
                        return (price != null ? price : 0.0) * i.getQuantity();
                    })
                    .sum();
            List<CouponService.CouponItem> couponItems = items.stream()
                    .map(i -> new CouponService.CouponItem(
                            i.getProductSnapshot().getId(),
                            i.getProductSnapshot().getCategoryId(),
                            (i.getProductSnapshot().getSellingPrice() != null
                                    && i.getProductSnapshot().getSellingPrice() > 0
                                            ? i.getProductSnapshot().getSellingPrice()
                                            : i.getProductSnapshot().getMrp()),
                            i.getQuantity()))
                    .collect(Collectors.toList());

            CouponValidationResponse couponResult = couponService.validateCoupon(rawCouponCode.trim(), couponItems,
                    itemsTotal,
                    userEmail);
            if (!couponResult.isValid()) {
                throw new ApiException(HttpStatus.BAD_REQUEST, couponResult.getMessage());
            }
            discountAmount = couponResult.getDiscountAmount();
            appliedCouponCode = couponResult.getCouponCode();
        }

        Order order = Order.builder()
                .id(generateOrderId())
                .userId(userEmail)
                .items(items)
                .addressSnapshot(addressSnapshot)
                .status("CREATED")
                .createdAt(LocalDateTime.now())
                .deliveryCharge(deliveryCharge)
                .couponCode(appliedCouponCode)
                .discountAmount(discountAmount)
                .build();

        Order savedOrder = orderRepository.save(order);
        OrderReceiptService.ReceiptDownload receiptDownload = null;

        try {
            receiptDownload = orderReceiptService.downloadReceipt(savedOrder);
        } catch (Exception e) {
            System.err.println("Failed to prepare order receipt: " + e.getMessage());
        }

        // Mark coupon as used AFTER order is committed
        if (appliedCouponCode != null) {
            try {
                couponService.markCouponUsed(appliedCouponCode, userEmail);
            } catch (Exception e) {
                System.err.println("Failed to mark coupon as used: " + e.getMessage());
            }
        }

        try {
            emailService.sendOrderConfirmationMail(
                    savedOrder,
                    receiptDownload != null ? receiptDownload.content() : null,
                    receiptDownload != null ? receiptDownload.fileName() : null);
        } catch (Exception e) {
            System.err.println("Failed to send order confirmation email: " + e.getMessage());
        }

        return populateReturnRequest(savedOrder);
    }

    private Order populateReturnRequest(Order order) {
        if (order != null && order.getId() != null) {
            List<ReturnRequest> returns = returnRequestRepository.findByOrderId(order.getId());
            if (returns != null && !returns.isEmpty()) {
                order.setReturnRequest(returns.get(returns.size() - 1));
            }
        }
        return order;
    }

    private void populateReturnRequestsBulk(Page<Order> res) {
        if (res == null || res.isEmpty()) return;
        
        Set<String> orderIds = res.stream().map(Order::getId).collect(Collectors.toSet());
        List<ReturnRequest> returns = returnRequestRepository.findByOrderIdIn(orderIds);
        
        java.util.Map<String, ReturnRequest> returnMap = new java.util.HashMap<>();
        for (ReturnRequest r : returns) {
            returnMap.put(r.getOrderId(), r); // This keeps the latest one encountered
        }
        
        res.forEach(order -> order.setReturnRequest(returnMap.get(order.getId())));
    }

    public Page<Order> getAllOrders(String keyword, String status, int page, int limit, String sortBy,
            String sortDirection, String userId, boolean isAdmin) {
        String sortField = (sortBy != null && !sortBy.isEmpty()) ? sortBy : "createdAt";
        Sort.Direction direction = Sort.Direction.DESC;
        if (sortDirection != null && sortDirection.equalsIgnoreCase("asc")) {
            direction = Sort.Direction.ASC;
        }
        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), limit, Sort.by(direction, sortField));

        if (keyword != null && !keyword.isBlank() && status != null && !status.isBlank()) {
            Page<Order> res = isAdmin
                    ? orderRepository.searchByKeywordAndStatus(keyword, status, pageable)
                    : orderRepository.searchByUserIdAndKeywordAndStatus(userId, keyword, status, pageable);
            populateReturnRequestsBulk(res);
            return res;
        }

        if (keyword != null && !keyword.isBlank()) {
            Page<Order> res = isAdmin
                    ? orderRepository.searchByKeyword(keyword, pageable)
                    : orderRepository.searchByUserIdAndKeyword(userId, keyword, pageable);
            populateReturnRequestsBulk(res);
            return res;
        }

        if (status != null && !status.isBlank()) {
            Page<Order> res = isAdmin
                    ? orderRepository.findByStatusIgnoreCase(status, pageable)
                    : orderRepository.findByUserIdAndStatusIgnoreCase(userId, status, pageable);
            populateReturnRequestsBulk(res);
            return res;
        }

        Page<Order> res = isAdmin
                ? orderRepository.findAll(pageable)
                : orderRepository.findByUserId(userId, pageable);
        populateReturnRequestsBulk(res);
        return res;
    }

    public Order getOrderById(String userEmail, String orderId, boolean isAdmin) {
        if (isAdmin) {
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new RuntimeException("Order not found!"));
            return populateReturnRequest(order);
        }
        Order order = orderRepository.findByUserIdAndId(userEmail, orderId)
                .orElseThrow(() -> new RuntimeException("Order not found!"));
        return populateReturnRequest(order);
    }

    public Order updateOrderAddress(String userEmail, String id, String addressId) {
        Order order = orderRepository.findByUserIdAndId(userEmail, id)
                .orElseThrow((() -> new RuntimeException("Order not found!")));
        OrderStatus status = OrderStatus.valueOf(order.getStatus());

        Set<OrderStatus> blockedStatuses = Set.of(
                OrderStatus.CANCELLED,
                OrderStatus.SHIPPED,
                OrderStatus.OUT_FOR_DELIVERY);
        if (blockedStatuses.contains(status)) {
            throw new RuntimeException("Order cannot be updated at this stage");
        }
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Cannot find the user"));
        Address address = addressRepository.findByIdAndUserId(addressId, user.getId())
                .orElseThrow(() -> new RuntimeException("Cannot find Address"));
        AddressSnapshot addressSnapshot = AddressSnapshot.from(address);
        order.setAddressSnapshot(addressSnapshot);

        return populateReturnRequest(orderRepository.save(order));
    }

    public void cancelOrder(String id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order Not Found"));

        if (order.getStatus().equals(OrderStatus.CANCELLED.name())) {
            throw new RuntimeException("Order is already cancelled");
        }

        // FIX: Using .equals() for String value comparison
        if (order.getStatus().equals(OrderStatus.SHIPPED.name()) ||
                order.getStatus().equals(OrderStatus.DELIVERED.name())) {
            throw new RuntimeException("Cannot cancel an order after it has been shipped.");
        }

        // Restore stock
        if (order.getItems() != null) {
            java.util.List<String> productIds = order.getItems().stream()
                .map(OrderItem::getProductSnapshot)
                .filter(java.util.Objects::nonNull)
                .map(ProductSnapshot::getId)
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toList());
                
            if (!productIds.isEmpty()) {
                java.util.List<Product> products = productRepository.findAllById(productIds);
                java.util.Map<String, Product> productMap = products.stream().collect(java.util.stream.Collectors.toMap(Product::getId, p -> p));
                
                for (OrderItem item : order.getItems()) {
                    ProductSnapshot snapshot = item.getProductSnapshot();
                    if (snapshot != null && snapshot.getId() != null) {
                        Product product = productMap.get(snapshot.getId());
                        if (product != null && product.isManageStock()) {
                            if (product.isHasVariants() && item.getVariantId() != null) {
                                product.getVariants().stream()
                                    .filter(v -> v.getId().equals(item.getVariantId()))
                                    .findFirst()
                                    .ifPresent(v -> {
                                        int currentQty = v.getStockQuantity() == null ? 0 : v.getStockQuantity();
                                        v.setStockQuantity(currentQty + item.getQuantity());
                                    });
                            } else {
                                int currentQty = product.getStockQuantity() == null ? 0 : product.getStockQuantity();
                                product.setStockQuantity(currentQty + item.getQuantity());
                            }
                        }
                    }
                }
                productRepository.saveAll(products);
            }
        }

        // Updating status
        order.setStatus(OrderStatus.CANCELLED.name());

        // Saving to the db
        orderRepository.save(order);

        // Send notification
        userRepository.findByEmail(order.getUserId()).ifPresent(user -> {
            String orderId = order.getId();
            fcmService.sendNotificationToUser(
                    user,
                    "Order Cancelled",
                    "Your order #" + orderId + " has been cancelled.",
                    null,
                    java.util.Map.of(
                            "orderId", orderId,
                            "type", "ORDER_UPDATE",
                            "click_action", "FLUTTER_NOTIFICATION_CLICK"));
            emailService.sendOrderStatusUpdateMail(order, "CANCELLED");
            whatsappService.sendOrderStatusUpdateWhatsapp(order, "CANCELLED");
        });
    }

    public Order updateOrderStatus(String id, String status) {

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order Not Found"));

        if (order.getStatus().equals("CANCELLED")) {
            throw new RuntimeException("order already cancelled");
        }

        try {
            OrderStatus orderStatus = OrderStatus.valueOf(status.toUpperCase());
            if (orderStatus == OrderStatus.CANCELLED && !order.getStatus().equals("CANCELLED")) {
                if (order.getItems() != null) {
                    java.util.List<String> productIds = order.getItems().stream()
                        .map(OrderItem::getProductSnapshot)
                        .filter(java.util.Objects::nonNull)
                        .map(ProductSnapshot::getId)
                        .filter(java.util.Objects::nonNull)
                        .collect(java.util.stream.Collectors.toList());
                        
                    if (!productIds.isEmpty()) {
                        java.util.List<Product> products = productRepository.findAllById(productIds);
                        java.util.Map<String, Product> productMap = products.stream().collect(java.util.stream.Collectors.toMap(Product::getId, p -> p));
                        
                        for (OrderItem item : order.getItems()) {
                            ProductSnapshot snapshot = item.getProductSnapshot();
                            if (snapshot != null && snapshot.getId() != null) {
                                Product product = productMap.get(snapshot.getId());
                                if (product != null && product.isManageStock()) {
                                    if (product.isHasVariants() && item.getVariantId() != null) {
                                        product.getVariants().stream()
                                            .filter(v -> v.getId().equals(item.getVariantId()))
                                            .findFirst()
                                            .ifPresent(v -> {
                                                int currentQty = v.getStockQuantity() == null ? 0 : v.getStockQuantity();
                                                v.setStockQuantity(currentQty + item.getQuantity());
                                            });
                                    } else {
                                        int currentQty = product.getStockQuantity() == null ? 0 : product.getStockQuantity();
                                        product.setStockQuantity(currentQty + item.getQuantity());
                                    }
                                }
                            }
                        }
                        productRepository.saveAll(products);
                    }
                }
            }
            order.setStatus(orderStatus.toString());
            Order savedOrder = orderRepository.save(order);

            // Send notification and email for all statuses except CONFIRMED
            if (orderStatus != OrderStatus.CONFIRMED) {
                userRepository.findByEmail(savedOrder.getUserId()).ifPresent(user -> {
                    String statusText = orderStatus.toString().replace("_", " ").toLowerCase();
                    // Capitalize first letter
                    statusText = statusText.substring(0, 1).toUpperCase() + statusText.substring(1);

                    String orderId = savedOrder.getId();
                    String title = "Order " + statusText + " (#" + orderId + ")";
                    String body = "Your order #" + orderId + " status has been updated to " + statusText + ".";

                    if (orderStatus == OrderStatus.SHIPPED) {
                        body = "Great news! Your order #" + orderId + " has been shipped.";
                    } else if (orderStatus == OrderStatus.DELIVERED) {
                        body = "Your order #" + orderId + " has been delivered successfully.";
                    } else if (orderStatus == OrderStatus.OUT_FOR_DELIVERY) {
                        body = "Your order #" + orderId + " is out for delivery!";
                    }

                    fcmService.sendNotificationToUser(
                            user,
                            title,
                            body,
                            null,
                            java.util.Map.of(
                                    "orderId", orderId,
                                    "type", "ORDER_UPDATE",
                                    "click_action", "FLUTTER_NOTIFICATION_CLICK"));

                    emailService.sendOrderStatusUpdateMail(savedOrder, orderStatus.toString());
                    whatsappService.sendOrderStatusUpdateWhatsapp(savedOrder, orderStatus.toString());
                });
            }

            return populateReturnRequest(savedOrder);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid order status: " + status);
        }
    }

    public long getCountByStatus(String status) {
        if (status == null || status.isBlank()) {
            throw new RuntimeException("Status is empty");
        }
        return orderRepository.countByStatus(status);
    }

    public OrderReceiptService.ReceiptDownload downloadReceipt(String userEmail, String orderId, boolean isAdmin) {
        Order order = getOrderById(userEmail, orderId, isAdmin);
        return orderReceiptService.downloadReceipt(order);
    }
}
