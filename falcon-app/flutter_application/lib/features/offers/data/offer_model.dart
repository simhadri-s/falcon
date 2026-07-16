import 'dart:convert';

enum OfferType { GENERAL, PRODUCT_BASED, CATEGORY_BASED }

enum DiscountType { FLAT, PERCENTAGE }

class Offer {
  final String id;
  final String name;
  final String description;
  final OfferType type;
  final DiscountType discountType;
  final double discountValue;
  final double minOrderValue;
  final List<String>? productIds;
  final List<String>? categoryIds;
  final DateTime? startDate;
  final DateTime? endDate;
  final bool isActive;
  final DateTime createdAt;

  Offer({
    required this.id,
    required this.name,
    required this.description,
    required this.type,
    required this.discountType,
    required this.discountValue,
    required this.minOrderValue,
    this.productIds,
    this.categoryIds,
    this.startDate,
    this.endDate,
    required this.isActive,
    required this.createdAt,
  });

  factory Offer.fromJson(Map<String, dynamic> json) {
    return Offer(
      id: json['id'] ?? '',
      name: json['name'] ?? '',
      description: json['description'] ?? '',
      type: OfferType.values.firstWhere(
        (e) => e.name == (json['type'] ?? 'GENERAL'),
        orElse: () => OfferType.GENERAL,
      ),
      discountType: DiscountType.values.firstWhere(
        (e) => e.name == (json['discountType'] ?? 'FLAT'),
        orElse: () => DiscountType.FLAT,
      ),
      discountValue: (json['discountValue'] ?? 0).toDouble(),
      minOrderValue: (json['minOrderValue'] ?? 0).toDouble(),
      productIds: json['productIds'] != null ? List<String>.from(json['productIds']) : null,
      categoryIds: json['categoryIds'] != null ? List<String>.from(json['categoryIds']) : null,
      startDate: json['startDate'] != null ? DateTime.parse(json['startDate']) : null,
      endDate: json['endDate'] != null ? DateTime.parse(json['endDate']) : null,
      isActive: json['isActive'] ?? false,
      createdAt: json['createdAt'] != null ? DateTime.parse(json['createdAt']) : DateTime.now(),
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'name': name,
      'description': description,
      'type': type.name,
      'discountType': discountType.name,
      'discountValue': discountValue,
      'minOrderValue': minOrderValue,
      'productIds': productIds,
      'categoryIds': categoryIds,
      'startDate': startDate?.toIso8601String(),
      'endDate': endDate?.toIso8601String(),
      'isActive': isActive,
    };
  }
}
