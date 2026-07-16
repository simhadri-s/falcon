import 'dart:convert';
import 'package:flutter/foundation.dart';
import 'package:flutter_application/core/network/api_client.dart';
import 'package:flutter_application/core/error/api_exception.dart';
import 'package:flutter_application/features/reviews/data/review_model.dart';
import 'package:flutter_application/core/constants/api_constants.dart';

class ReviewResponse {
  final bool success;
  final String message;
  final Review? review;

  ReviewResponse({required this.success, required this.message, this.review});
}

class ReviewsApiService {
  static String get baseUrl => '${ApiConstants.baseUrl}/reviews';

  Future<PaginatedReviews> getProductReviews(String productId, {int page = 1, int limit = 10}) async {
    try {
      final response = await ApiClient.get(
        Uri.parse('$baseUrl/product/$productId?page=$page&limit=$limit'),
      );

      if (response.statusCode == 200) {
        final Map<String, dynamic> body = json.decode(response.body);
        final List<dynamic> data = body['data'] ?? [];
        
        return PaginatedReviews(
          reviews: data.map((json) => Review.fromJson(json)).toList(),
          total: body['total'] ?? 0,
          pages: body['pages'] ?? 1,
        );
      }
      throw ApiException.fromResponse(response);
    } catch (e) {
      debugPrint('Error fetching reviews: $e');
      if (e is ApiException) rethrow;
      throw ApiException('Failed to fetch reviews.');
    }
  }

  /// Fetches the current user's review for a specific product.
  /// Returns null if the user hasn't reviewed this product yet.
  Future<Review?> getUserReview(String productId) async {
    try {
      final token = await ApiClient.getToken();
      if (token == null) return null;

      final response = await ApiClient.get(
        Uri.parse('$baseUrl/my/$productId'),
      );

      if (response.statusCode == 200) {
        final Map<String, dynamic> body = json.decode(response.body);
        return Review.fromJson(body);
      }
      // 204 No Content means no review exists
      if (response.statusCode == 204) {
        return null;
      }
      return null;
    } catch (e) {
      debugPrint('Error fetching user review: $e');
      return null;
    }
  }

  /// Checks if the current user is eligible to review a product (has purchased it).
  Future<bool> checkEligibility(String productId) async {
    try {
      final token = await ApiClient.getToken();
      if (token == null) return false;

      final response = await ApiClient.get(
        Uri.parse('$baseUrl/check-eligibility/$productId'),
      );

      if (response.statusCode == 200) {
        final Map<String, dynamic> body = json.decode(response.body);
        return body['eligible'] ?? false;
      }
      return false;
    } catch (e) {
      debugPrint('Error checking review eligibility: $e');
      return false;
    }
  }

  Future<ReviewResponse> addReview({
    required String productId,
    required int rating,
    required String comment,
  }) async {
    try {
      final token = await ApiClient.getToken();
      if (token == null) {
        return ReviewResponse(
          success: false,
          message: 'Authentication required to submit a review.',
        );
      }

      final body = <String, dynamic>{
        'productId': productId,
        'rating': rating,
        'comment': comment,
      };

      final response = await ApiClient.post(
        Uri.parse(baseUrl),
        body: json.encode(body),
      );

      if (response.statusCode == 201 || response.statusCode == 200) {
        final Map<String, dynamic> responseData = json.decode(response.body);
        return ReviewResponse(
          success: true, 
          message: 'Review submitted successfully',
          review: Review.fromJson(responseData),
        );
      }
      if (response.statusCode == 401) {
        return ReviewResponse(success: false, message: 'Authentication required to submit a review.');
      }

      return ReviewResponse(
        success: false,
        message: ApiException.fromResponse(response).message,
      );
    } catch (e) {
      debugPrint('Error adding review: $e');
      if (e is ApiException) {
        return ReviewResponse(success: false, message: e.message);
      }
      return ReviewResponse(
        success: false,
        message: 'Connection error. Please check your internet.',
      );
    }
  }

  Future<ReviewResponse> updateReview({
    required String productId,
    required int rating,
    required String comment,
  }) async {
    try {
      final token = await ApiClient.getToken();
      if (token == null) {
        return ReviewResponse(
          success: false,
          message: 'Authentication required to update a review.',
        );
      }

      final body = <String, dynamic>{
        'productId': productId,
        'rating': rating,
        'comment': comment,
      };

      final response = await ApiClient.put(
        Uri.parse(baseUrl),
        body: json.encode(body),
      );

      if (response.statusCode == 200) {
        final Map<String, dynamic> responseData = json.decode(response.body);
        return ReviewResponse(
          success: true,
          message: 'Review updated successfully',
          review: Review.fromJson(responseData),
        );
      }
      if (response.statusCode == 401) {
        return ReviewResponse(success: false, message: 'Authentication required to update a review.');
      }

      return ReviewResponse(
        success: false,
        message: ApiException.fromResponse(response).message,
      );
    } catch (e) {
      debugPrint('Error updating review: $e');
      if (e is ApiException) {
        return ReviewResponse(success: false, message: e.message);
      }
      return ReviewResponse(
        success: false,
        message: 'Connection error. Please check your internet.',
      );
    }
  }
}
