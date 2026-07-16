import 'package:flutter_application/features/home/data/product_model.dart';
import 'cart_item_model.dart';

class Cart {
  final String id;
  final String userId;
  final List<CartItem> items;
  final int totalItems;
  final double subtotal;
  final double totalDiscount;
  final double totalAmount;

  Cart({
    required this.id,
    required this.userId,
    required this.items,
    required this.totalItems,
    required this.subtotal,
    required this.totalDiscount,
    required this.totalAmount,
  });

  factory Cart.fromJson(Map<String, dynamic> json, List<CartItem> cartItems) {
    return Cart(
      id: json['id'] ?? '',
      userId: json['userId'] ?? '',
      items: cartItems,
      totalItems: json['totalItems'] ?? 0,
      subtotal: (json['subtotal'] ?? 0).toDouble(),
      totalDiscount: (json['totalDiscount'] ?? 0).toDouble(),
      totalAmount: (json['totalAmount'] ?? 0).toDouble(),
    );
  }
}
