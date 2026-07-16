import 'dart:convert';
import 'package:flutter/foundation.dart';
import 'package:flutter_application/core/error/api_exception.dart';
import 'package:flutter_application/core/network/api_client.dart';
import 'package:flutter_application/features/cart/data/cart_item_model.dart';
import 'package:flutter_application/features/cart/data/cart_model.dart';
import 'package:flutter_application/features/home/data/product_model.dart';
import 'package:flutter_application/features/products/data/products_api_service.dart';
import 'package:flutter_application/core/constants/api_constants.dart';

class CartService {
  static String get baseUrl => '${ApiConstants.baseUrl}/cart';

  double _asDouble(dynamic value) {
    if (value is num) {
      return value.toDouble();
    }
    return double.tryParse(value?.toString() ?? '') ?? 0.0;
  }

  Future<Cart?> getCartDetails() async {
    return await tryGetCart();
  }

  Future<Cart?> tryGetCart() async {
    try {
      final token = await ApiClient.getToken();
      if (token == null || token.isEmpty) return null;

      final response = await ApiClient.get(Uri.parse(baseUrl));

      if (response.statusCode == 200) {
        final Map<String, dynamic> decoded = json.decode(response.body);
        final List<dynamic> itemsData = decoded['items'] ?? [];

        final ProductsApiService productsApiService = ProductsApiService();
        final List<CartItem> cartItems = [];

        for (final item in itemsData) {
          final String productId = item['productId'] ?? '';
          final String productSlug = item['productSlug'] ?? '';
          final int quantity = item['quantity'] ?? 1;
          final bool active = item['active'] ?? true;
          final String? variantId = item['variantId'];
          final Map<String, String>? variantAttributes = item['variantAttributes'] != null ? Map<String, String>.from(item['variantAttributes']) : null;
          final double? variantPrice = item['variantPrice'] != null ? _asDouble(item['variantPrice']) : null;

          Product? fullProduct;
          final String identifier = productSlug.isNotEmpty
              ? productSlug
              : productId;

          if (identifier.isNotEmpty) {
            try {
              fullProduct = await productsApiService.getProductById(identifier);
            } catch (e) {
              debugPrint('CartService: Error fetching product $identifier: $e');
            }
          }

          if (fullProduct == null) {
            final double sellingPrice = _asDouble(item['sellingPrice']);
            final double mrp = _asDouble(item['mrp']);
            fullProduct = Product(
              id: productId,
              name: item['productName'] ?? 'Unknown Product',
              category: '',
              description: '',
              imageUrls: [],
              featured: false,
              price: sellingPrice > 0 ? sellingPrice : mrp,
              mrp: mrp,
              sellingPrice: sellingPrice,
            );
          }

          cartItems.add(
            CartItem(
              product: fullProduct,
              quantity: quantity,
              active: active,
              variantId: variantId,
              variantAttributes: variantAttributes,
              variantPrice: variantPrice,
            ),
          );
        }

        return Cart.fromJson(decoded, cartItems);
      }
      return null;
    } catch (e) {
      debugPrint('CartService: Error fetching cart: $e');
      return null;
    }
  }

  Future<void> addToCart(String productId, int quantity, {String? variantId}) async {
    try {
      final sanitizedProductId = productId.trim();
      if (sanitizedProductId.isEmpty || quantity <= 0) {
        throw ApiException(
          'Invalid cart request. Please try again.',
          statusCode: 400,
        );
      }

      final body = json.encode({
        'productId': sanitizedProductId,
        'quantity': quantity,
        if (variantId != null) 'variantId': variantId,
      });

      for (final url in [baseUrl, '$baseUrl/add']) {
        final response = await ApiClient.post(
          Uri.parse(url),
          body: body,
        );

        if (_isSuccessfulResponse(response)) {
          return;
        }

        if (response.statusCode != 404 && response.statusCode != 405) {
          if (response.statusCode == 401) {
            throw ApiException('Authentication required. Please login again.', statusCode: 401);
          }
          throw ApiException.fromResponse(response);
        }
      }

      throw ApiException('Cart endpoint not found. Please try again later.');
    } catch (e) {
      debugPrint('CartService: Error adding to cart: $e');
      if (e is ApiException) rethrow;
      throw ApiException('Failed to add item to cart. Please try again.');
    }
  }

  Future<void> updateQuantity(String productId, int quantity, {String? variantId}) async {
    try {
      final bodyMap = <String, dynamic>{'productId': productId, 'quantity': quantity};
      if (variantId != null) bodyMap['variantId'] = variantId;
      
      final response = await ApiClient.put(
        Uri.parse(baseUrl),
        body: json.encode(bodyMap),
      );

      if (response.statusCode == 200) {
        return;
      }
      if (response.statusCode == 401) {
        throw ApiException('Authentication required. Please login again.', statusCode: 401);
      }
      throw ApiException.fromResponse(response);
    } catch (e) {
      debugPrint('Error updating quantity: $e');
      if (e is ApiException) rethrow;
      throw ApiException('Failed to update cart. Please try again.');
    }
  }

  Future<void> deleteItem(String productId, {String? variantId}) async {
    try {
      final url = variantId != null ? '$baseUrl/$productId?variantId=$variantId' : '$baseUrl/$productId';
      final response = await ApiClient.delete(
        Uri.parse(url),
      );

      if (response.statusCode == 200 || response.statusCode == 204) {
        return;
      }
      if (response.statusCode == 401) {
        throw ApiException('Authentication required. Please login again.', statusCode: 401);
      }
      throw ApiException.fromResponse(response);
    } catch (e) {
      debugPrint('Error deleting from cart: $e');
      if (e is ApiException) rethrow;
      throw ApiException('Failed to remove item from cart. Please try again.');
    }
  }

  Future<void> clearCart() async {
    try {
      final response = await ApiClient.delete(
        Uri.parse(baseUrl),
      );

      if (response.statusCode == 200 || response.statusCode == 204) {
        return;
      }
      if (response.statusCode == 401) {
        throw ApiException('Authentication required. Please login again.', statusCode: 401);
      }
      throw ApiException.fromResponse(response);
    } catch (e) {
      debugPrint('Error clearing cart: $e');
      if (e is ApiException) rethrow;
      throw ApiException('Failed to clear cart. Please try again.');
    }
  }

  Future<void> updateActiveStatus(String productId, bool active, {String? variantId}) async {
    try {
      final url = variantId != null ? '$baseUrl/item/$productId/active?variantId=$variantId' : '$baseUrl/item/$productId/active';
      final response = await ApiClient.put(
        Uri.parse(url),
        body: json.encode(active),
      );

      if (response.statusCode != 200) {
        if (response.statusCode == 401) {
          throw ApiException('Authentication required. Please login again.', statusCode: 401);
        }
        throw ApiException.fromResponse(response);
      }
    } catch (e) {
      debugPrint('Error updating active status: $e');
      if (e is ApiException) rethrow;
      throw ApiException('Failed to update item status');
    }
  }

  Future<void> updateAllActiveStatus(bool active) async {
    try {
      final response = await ApiClient.put(
        Uri.parse('$baseUrl/active'),
        body: json.encode(active),
      );

      if (response.statusCode != 200) {
        if (response.statusCode == 401) {
          throw ApiException('Authentication required. Please login again.', statusCode: 401);
        }
        throw ApiException.fromResponse(response);
      }
    } catch (e) {
      debugPrint('Error updating all active status: $e');
      if (e is ApiException) rethrow;
      throw ApiException('Failed to update cart status');
    }
  }

  bool _isSuccessfulResponse(dynamic response) {
    if (response.statusCode != 200 &&
        response.statusCode != 201 &&
        response.statusCode != 204) {
      return false;
    }

    if (response.body.isEmpty) {
      return true;
    }

    try {
      final decoded = json.decode(response.body);
      if (decoded is Map<String, dynamic> && decoded['success'] is bool) {
        return decoded['success'] as bool;
      }
    } catch (_) {
    }

    return true;
  }
}
