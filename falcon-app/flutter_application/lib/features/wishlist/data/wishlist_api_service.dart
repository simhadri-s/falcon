import 'dart:convert';
import 'package:flutter_application/core/network/api_client.dart';
import 'package:flutter_application/core/error/api_exception.dart';
import 'package:flutter_application/features/home/data/product_model.dart';
import 'package:flutter_application/core/constants/api_constants.dart';

class WishlistResponse {
  final String id;
  final String userId;
  final List<Product> products;
  final int totalItems;

  WishlistResponse({
    required this.id,
    required this.userId,
    required this.products,
    required this.totalItems,
  });

  factory WishlistResponse.fromJson(Map<String, dynamic> json) {
    return WishlistResponse(
      id: json['id'] ?? '',
      userId: json['userId'] ?? '',
      products: (json['products'] as List<dynamic>?)
              ?.map((item) => Product.fromJson(item))
              .toList() ??
          [],
      totalItems: json['totalItems'] ?? 0,
    );
  }
}

class WishlistApiService {
  static String get baseUrl => ApiConstants.baseUrl;

  Future<WishlistResponse> getWishlist() async {
    try {
      final response = await ApiClient.get(
        Uri.parse('$baseUrl/wishlists'),
      );

      if (response.statusCode == 200) {
        return WishlistResponse.fromJson(json.decode(response.body));
      } else {
        throw ApiException.fromResponse(response);
      }
    } catch (e) {
      if (e is ApiException) rethrow;
      throw ApiException('Failed to load wishlist: $e');
    }
  }

  Future<WishlistResponse> toggleWishlist(String productId) async {
    try {
      final response = await ApiClient.post(
        Uri.parse('$baseUrl/wishlists/toggle'),
        body: json.encode({'productId': productId}),
      );

      if (response.statusCode == 200) {
        return WishlistResponse.fromJson(json.decode(response.body));
      } else {
        throw ApiException.fromResponse(response);
      }
    } catch (e) {
      if (e is ApiException) rethrow;
      throw ApiException('Failed to toggle wishlist: $e');
    }
  }
}
