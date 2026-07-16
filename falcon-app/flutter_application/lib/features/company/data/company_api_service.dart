import 'dart:convert';
import 'package:flutter_application/core/network/api_client.dart';
import 'package:flutter/foundation.dart';
import 'company_models.dart';
import 'package:flutter_application/core/constants/api_constants.dart';

class CompanyApiService {
  static String get baseUrl => ApiConstants.baseUrl;

  Future<List<CompanyImage>> getCompanyImages() async {
    try {
      final response = await ApiClient.get(Uri.parse('$baseUrl/company-images'));
      if (response.statusCode == 200) {
        final dynamic decoded = json.decode(response.body);
        List<dynamic> data = [];
        
        if (decoded is List) {
          data = decoded;
        } else if (decoded is Map && decoded.containsKey('data')) {
          data = decoded['data'] as List<dynamic>;
        }
        
        return data.map((item) => CompanyImage.fromJson(item as Map<String, dynamic>)).toList();
      }
      return [];
    } catch (e) {
      debugPrint('Error fetching company images: $e');
      return [];
    }
  }

  Future<CompanySettings?> getCompanySettings() async {
    try {
      final response = await ApiClient.get(Uri.parse('$baseUrl/company-settings'));
      if (response.statusCode == 200) {
        final dynamic decoded = json.decode(response.body);
        Map<String, dynamic> data = {};
        
        if (decoded is Map) {
          if (decoded.containsKey('data') && decoded['data'] is Map) {
            data = decoded['data'] as Map<String, dynamic>;
          } else {
            data = decoded as Map<String, dynamic>;
          }
        }
        
        return CompanySettings.fromJson(data);
      }
      return null;
    } catch (e) {
      debugPrint('Error fetching company settings: $e');
      return null;
    }
  }
}
