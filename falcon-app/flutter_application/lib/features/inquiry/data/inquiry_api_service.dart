import 'dart:convert';
import 'package:flutter_application/core/network/api_client.dart';
import 'package:flutter_application/core/error/api_exception.dart';
import 'package:flutter_application/core/constants/api_constants.dart';

class InquiryApiService {
  static String get baseUrl => ApiConstants.baseUrl;

  Future<Map<String, dynamic>> createInquiry({
    required String name,
    required String email,
    required String subject,
    required String message,
    required String phone,
  }) async {
    try {
      final url = Uri.parse('$baseUrl/inquiries');
      final response = await ApiClient.post(
        url,
        body: json.encode({
          'name': name,
          'email': email,
          'subject': subject,
          'message': message,
          'phone': phone,
        }),
      );

      if (response.statusCode == 201 || response.statusCode == 200) {
        return json.decode(response.body);
      }

      throw ApiException.fromResponse(response);
    } catch (e) {
      if (e is ApiException) rethrow;
      throw ApiException('Failed to submit inquiry. Please try again.');
    }
  }
}
