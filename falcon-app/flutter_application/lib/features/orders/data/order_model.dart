import 'package:flutter_application/features/orders/data/return_model.dart';

enum OrderStatus { created, processing, shipped, delivered, cancelled }

class OrderAddress {
  final String fullName;
  final String phoneNumber;
  final String street;
  final String city;
  final String pincode;
  final String country;

  OrderAddress({
    required this.fullName,
    required this.phoneNumber,
    required this.street,
    required this.city,
    required this.pincode,
    required this.country,
  });

  factory OrderAddress.fromJson(Map<String, dynamic> json) {
    return OrderAddress(
      fullName: json['fullName'] ?? '',
      phoneNumber: json['phoneNumber'] ?? '',
      street: json['street'] ?? '',
      city: json['city'] ?? '',
      pincode: json['pincode'] ?? '',
      country: json['country'] ?? '',
    );
  }
}

class Order {
  final String id;
  final String userId;
  final List<OrderItem> items;
  final DateTime createdAt;
  final OrderStatus status;
  final OrderAddress? address;
  final double deliveryCharge;
  final String? couponCode;
  final double discountAmount;
  final ReturnRequest? returnRequest;

  Order({
    required this.id,
    required this.userId,
    required this.items,
    required this.createdAt,
    required this.status,
    this.address,
    required this.deliveryCharge,
    this.couponCode,
    this.discountAmount = 0.0,
    this.returnRequest,
  });

  factory Order.fromJson(Map<String, dynamic> json) {
    return Order(
      id: json['id'] ?? '',
      userId: json['userId'] ?? '',
      items: (json['items'] as List?)?.map((i) => OrderItem.fromJson(i)).toList() ?? [],
      createdAt: _parseDate(json['createdAt']),
      status: _parseStatus(json['status']),
      address: json['addressSnapshot'] != null
          ? OrderAddress.fromJson(json['addressSnapshot'])
          : null,
      deliveryCharge: _asDouble(json['deliveryCharge']),
      couponCode: json['couponCode'],
      discountAmount: _asDouble(json['discountAmount']),
      returnRequest: json['returnRequest'] != null 
          ? ReturnRequest.fromJson(json['returnRequest']) 
          : null,
    );
  }

  static double _asDouble(dynamic value) {
    if (value is num) {
      return value.toDouble();
    }
    return double.tryParse(value?.toString() ?? '') ?? 0.0;
  }

  static DateTime _parseDate(String? dateStr) {
    if (dateStr == null) return DateTime.now();
    try {
      return DateTime.parse(dateStr);
    } catch (_) {
      return DateTime.now();
    }
  }

  static OrderStatus _parseStatus(String? status) {
    switch (status?.toUpperCase()) {
      case 'CREATED': return OrderStatus.created;
      case 'PROCESSING': return OrderStatus.processing;
      case 'SHIPPED': return OrderStatus.shipped;
      case 'DELIVERED': return OrderStatus.delivered;
      case 'CANCELLED': return OrderStatus.cancelled;
      default: return OrderStatus.created;
    }
  }

  String get statusDisplay {
    switch (status) {
      case OrderStatus.created: return 'Created';
      case OrderStatus.processing: return 'Processing';
      case OrderStatus.shipped: return 'Shipped';
      case OrderStatus.delivered: return 'Delivered';
      case OrderStatus.cancelled: return 'Cancelled';
    }
  }

  double get totalAmount => (items.fold(0.0, (sum, item) => sum + item.total) + deliveryCharge - discountAmount).clamp(0, double.infinity);
}

class OrderItemProduct {
  final String id;
  final String name;
  final String productCode;
  final String description;
  final List<String> imageUrls;
  final double price;
  final double mrp;
  final double sellingPrice;

  OrderItemProduct({
    required this.id,
    required this.name,
    required this.productCode,
    required this.description,
    required this.imageUrls,
    this.price = 0.0,
    this.mrp = 0.0,
    this.sellingPrice = 0.0,
  });

  bool get hasPrice => price > 0;
  bool get hasMrp => mrp > 0;
  bool get hasSellingPrice => sellingPrice > 0;
  bool get hasDiscount => hasSellingPrice && hasMrp && mrp > sellingPrice;

  factory OrderItemProduct.fromJson(Map<String, dynamic> json) {
    final double parsedSellingPrice = _asDouble(json['sellingPrice']);
    final double parsedMrp = _asDouble(json['mrp']);
    final double legacyPrice = _asDouble(json['price']);

    return OrderItemProduct(
      id: json['id'] ?? '',
      name: json['name'] ?? '',
      productCode: json['productCode'] ?? '',
      description: json['description'] ?? '',
      imageUrls: List<String>.from(json['imageUrls'] ?? []),
      price: parsedSellingPrice > 0
          ? parsedSellingPrice
          : (parsedMrp > 0 ? parsedMrp : legacyPrice),
      mrp: parsedMrp,
      sellingPrice: parsedSellingPrice,
    );
  }

  static double _asDouble(dynamic value) {
    if (value is num) {
      return value.toDouble();
    }
    return double.tryParse(value?.toString() ?? '') ?? 0.0;
  }
}

class OrderItem {
  final String id;
  final OrderItemProduct product;
  final int quantity;
  final String? variantId;
  final Map<String, String>? variantAttributes;

  OrderItem({
    required this.id,
    required this.product,
    required this.quantity,
    this.variantId,
    this.variantAttributes,
  });

  factory OrderItem.fromJson(Map<String, dynamic> json) {
    return OrderItem(
      id: json['id'] ?? '',
      product: OrderItemProduct.fromJson(json['productSnapshot'] ?? {}),
      quantity: json['quantity'] ?? 0,
      variantId: json['variantId'],
      variantAttributes: json['variantAttributes'] != null ? Map<String, String>.from(json['variantAttributes']) : null,
    );
  }

  double get total => product.price * quantity;
}
