import 'package:flutter_application/features/home/data/product_model.dart';

class CartItem {
  final Product product;
  int quantity;
  final bool active;
  final String? variantId;
  final Map<String, String>? variantAttributes;
  final double? variantPrice;

  CartItem({
    required this.product,
    this.quantity = 1,
    this.active = true,
    this.variantId,
    this.variantAttributes,
    this.variantPrice,
  });

  double get price => variantPrice ?? product.price;
  double get total => price * quantity;
}
