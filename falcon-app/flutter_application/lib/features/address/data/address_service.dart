import 'dart:convert';
import 'package:flutter/foundation.dart';
import 'package:flutter_application/core/network/api_client.dart';
import 'address_model.dart';
import 'package:flutter_application/core/error/api_exception.dart';
import 'package:flutter_application/core/constants/api_constants.dart';

class AddressService {
  static String get baseUrl => '${ApiConstants.baseUrl}/address';

  Future<List<Address>> getAddresses() async {
    try {
      final response = await ApiClient.get(
        Uri.parse(baseUrl),
      );

      debugPrint('getAddresses status: ${response.statusCode}');
      debugPrint('getAddresses body: ${response.body}');

      if (response.statusCode == 200) {
        final decoded = json.decode(response.body);
        // Handle both list and paginated response
        List<dynamic> data;
        if (decoded is List) {
          data = decoded;
        } else if (decoded is Map && decoded.containsKey('data')) {
          data = decoded['data'] ?? [];
        } else {
          data = [];
        }
        return data.map((json) => Address.fromJson(json)).toList();
      }
      if (response.statusCode == 401) {
        throw ApiException('Authentication required. Please login again.', statusCode: 401);
      }
      throw ApiException.fromResponse(response);
    } catch (e) {
      debugPrint('Error fetching addresses: $e');
      if (e is ApiException) rethrow;
      throw ApiException('Failed to fetch addresses. Please try again.');
    }
  }

  Future<bool> createAddress({
    required String fullName,
    required String phoneNumber,
    required String street,
    required String city,
    required String state,
    required String pincode,
    required String country,
    bool isDefault = false,
  }) async {
    return _saveAddress(
      url: baseUrl,
      method: 'POST',
      fullName: fullName,
      phoneNumber: phoneNumber,
      street: street,
      city: city,
      state: state,
      pincode: pincode,
      country: country,
      isDefault: isDefault,
    );
  }

  Future<bool> updateAddress({
    required String id,
    required String fullName,
    required String phoneNumber,
    required String street,
    required String city,
    required String state,
    required String pincode,
    required String country,
    bool isDefault = false,
  }) async {
    return _saveAddress(
      url: '$baseUrl/$id',
      method: 'PUT',
      fullName: fullName,
      phoneNumber: phoneNumber,
      street: street,
      city: city,
      state: state,
      pincode: pincode,
      country: country,
      isDefault: isDefault,
    );
  }

  Future<bool> _saveAddress({
    required String url,
    required String method,
    required String fullName,
    required String phoneNumber,
    required String street,
    required String city,
    required String state,
    required String pincode,
    required String country,
    required bool isDefault,
  }) async {
    try {
      final body = json.encode({
        'fullName': fullName,
        'phoneNumber': phoneNumber,
        'street': street,
        'city': city,
        'state': state,
        'pincode': pincode,
        'country': country,
        'default': isDefault,
      });

      dynamic response;
      if (method == 'PUT') {
        response = await ApiClient.put(Uri.parse(url), body: body);
      } else {
        response = await ApiClient.post(
          Uri.parse(url),
          body: body,
        );
      }

      if (response.statusCode == 200 || response.statusCode == 201) {
        return true;
      } else {
        if (response.statusCode == 401) {
          throw ApiException('Authentication required. Please login again.', statusCode: 401);
        }
        throw ApiException.fromResponse(response);
      }
    } catch (e) {
      if (e is ApiException) rethrow;
      throw ApiException('Failed to save address. Please try again.');
    }
  }
}
