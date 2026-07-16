import 'dart:convert';
import 'package:flutter/foundation.dart';
import 'package:flutter_application/core/network/api_client.dart';
import 'package:flutter_application/core/constants/api_constants.dart';

class CouponValidationResult {
  final bool valid;
  final double discountAmount;
  final String message;
  final String? couponCode;

  const CouponValidationResult({
    required this.valid,
    required this.discountAmount,
    required this.message,
    this.couponCode,
  });

  factory CouponValidationResult.fromJson(Map<String, dynamic> json) {
    return CouponValidationResult(
      valid: json['valid'] as bool? ?? false,
      discountAmount: (json['discountAmount'] as num?)?.toDouble() ?? 0.0,
      message: json['message'] as String? ?? '',
      couponCode: json['couponCode'] as String?,
    );
  }
}

class CouponService {

  Future<CouponValidationResult> validateCoupon({
    required String code,
    required double orderTotal,
    List<Map<String, dynamic>>? items,
  }) async {
    try {
      final token = await ApiClient.getToken();
      if (token == null) {
        return const CouponValidationResult(
          valid: false,
          discountAmount: 0.0,
          message: 'Authentication required. Please log in again.',
        );
      }

      final response = await ApiClient.post(
        Uri.parse('${ApiConstants.baseUrl}/coupons/validate'),
        body: json.encode({
          'code': code.trim().toUpperCase(),
          'orderTotal': orderTotal,
          if (items != null) 'items': items,
        }),
      );

      final body = json.decode(response.body);

      if (response.statusCode == 200) {
        return CouponValidationResult.fromJson(body as Map<String, dynamic>);
      }

      // Backend returned an error — extract message
      final errorMsg = (body is Map ? body['message'] ?? body['error'] : null) as String?;
      return CouponValidationResult(
        valid: false,
        discountAmount: 0.0,
        message: errorMsg ?? 'Invalid coupon code.',
      );
    } catch (e) {
      debugPrint('CouponService.validateCoupon error: $e');
      return const CouponValidationResult(
        valid: false,
        discountAmount: 0.0,
        message: 'Could not validate coupon. Please try again.',
      );
    }
  }
}
