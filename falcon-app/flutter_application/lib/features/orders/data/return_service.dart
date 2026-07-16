import 'dart:convert';
import 'package:flutter/foundation.dart';
import 'package:flutter_application/core/network/api_client.dart';
import 'package:flutter_application/core/error/api_exception.dart';
import 'package:flutter_application/core/constants/api_constants.dart';
import 'package:flutter_application/features/orders/data/return_model.dart';

class ReturnService {
  static String get baseUrl => '${ApiConstants.baseUrl}/returns';

  Future<List<ReturnRequest>> getMyReturns() async {
    try {
      final response = await ApiClient.get(
        Uri.parse('$baseUrl/my'),
      );

      if (response.statusCode == 200) {
        final List<dynamic> data = json.decode(response.body);
        return data.map((json) => ReturnRequest.fromJson(json)).toList();
      }
      if (response.statusCode == 401) {
        throw ApiException('Authentication required', statusCode: 401);
      }
      throw ApiException.fromResponse(response);
    } catch (e) {
      debugPrint('Error fetching returns: $e');
      rethrow;
    }
  }

  Future<ReturnRequest> createReturnRequest({
    required String orderId,
    required List<Map<String, dynamic>> items,
    required String reason,
    required String comment,
  }) async {
    try {
      final response = await ApiClient.post(
        Uri.parse(baseUrl),
        body: json.encode({
          'orderId': orderId,
          'items': items,
          'reason': reason,
          'comment': comment,
        }),
      );

      if (response.statusCode == 200 || response.statusCode == 201) {
        return ReturnRequest.fromJson(json.decode(response.body));
      }
      if (response.statusCode == 401) {
        throw ApiException('Authentication required', statusCode: 401);
      }
      throw ApiException.fromResponse(response);
    } catch (e) {
      debugPrint('Error creating return request: $e');
      rethrow;
    }
  }
}
