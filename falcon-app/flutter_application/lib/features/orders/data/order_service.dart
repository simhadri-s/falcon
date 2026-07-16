import 'dart:convert';
import 'package:flutter/foundation.dart';
import 'package:file_picker/file_picker.dart';
import 'package:flutter_application/core/network/api_client.dart';
import 'package:flutter_application/core/error/api_exception.dart';
import 'package:flutter_application/features/orders/data/order_model.dart';
import 'package:flutter_application/core/constants/api_constants.dart';

class OrderResponse {
  final bool success;
  final String message;

  OrderResponse({required this.success, required this.message});
}

class ReceiptDownloadResult {
  final String fileName;
  final String? savedPath;
  final bool didSave;

  ReceiptDownloadResult({
    required this.fileName,
    required this.savedPath,
    required this.didSave,
  });
}

class OrderService {
  static String get baseUrl => '${ApiConstants.baseUrl}/orders';

  Future<OrderResponse> placeOrder(List<Map<String, dynamic>> items, String addressId, {String? couponCode}) async {
    try {
      if (items.isEmpty) {
        return OrderResponse(success: false, message: 'Your cart is empty.');
      }

      final body = <String, dynamic>{
        'items': items,
        'addressId': addressId,
      };
      if (couponCode != null && couponCode.trim().isNotEmpty) {
        body['couponCode'] = couponCode.trim().toUpperCase();
      }

      final response = await ApiClient.post(
        Uri.parse(baseUrl),
        body: json.encode(body),
      );

      final String message = _messageFromResponse(
        response,
        successFallback: 'Order placed successfully!',
      );

      if (response.statusCode == 200 || response.statusCode == 201) {
        return OrderResponse(success: true, message: message);
      }
      if (response.statusCode == 401) {
        return OrderResponse(success: false, message: 'Authentication required. Please login again.');
      }

      return OrderResponse(
        success: false,
        message: ApiException.fromResponse(response).message,
      );
    } catch (e) {
      debugPrint('Error placing order: $e');
      if (e is ApiException) {
        return OrderResponse(success: false, message: e.message);
      }
      return OrderResponse(
        success: false,
        message: 'Connection error. Please check your internet.',
      );
    }
  }

  Future<List<Order>> getMyOrders() async {
    try {
      final response = await ApiClient.get(
        Uri.parse(baseUrl),
      );

      debugPrint('getMyOrders status: ${response.statusCode}');
      debugPrint('getMyOrders body: ${response.body}');

      if (response.statusCode == 200) {
        final Map<String, dynamic> body = json.decode(response.body);
        final List<dynamic> data = body['data'] ?? [];
        return data.map((json) => Order.fromJson(json)).toList();
      }
      if (response.statusCode == 401) {
        throw ApiException('Authentication required. Please login again.', statusCode: 401);
      }
      throw ApiException.fromResponse(response);
    } catch (e) {
      debugPrint('Error fetching orders: $e');
      if (e is ApiException) rethrow;
      throw ApiException('Failed to fetch orders. Please try again.');
    }
  }

  Future<OrderResponse> updateOrderAddress(String orderId, String addressId) async {
    try {
      // Explicitly building the URI to avoid any parsing issues
      final String url = '$baseUrl/$orderId/address?addressId=$addressId';
      final uri = Uri.parse(url);

      debugPrint('Calling Update Address: $uri');

      final response = await ApiClient.put(
        uri,
      );

      if (response.statusCode == 200) {
        return OrderResponse(
          success: true,
          message: _messageFromResponse(response, successFallback: 'Address updated successfully'),
        );
      }
      if (response.statusCode == 401) {
        return OrderResponse(success: false, message: 'Authentication required.');
      }

      return OrderResponse(
        success: false,
        message: ApiException.fromResponse(response).message,
      );
    } catch (e) {
      debugPrint('Error updating order address: $e');
      return OrderResponse(success: false, message: 'Error updating address.');
    }
  }

  Future<Order?> getOrderById(String orderId) async {
    try {
      final response = await ApiClient.get(
        Uri.parse('$baseUrl/$orderId'),
      );

      if (response.statusCode == 200) {
        final data = json.decode(response.body);
        return Order.fromJson(data);
      }
      return null;
    } catch (e) {
      debugPrint('Error fetching order by ID: $e');
      return null;
    }
  }

  Future<OrderResponse> updateOrder(
    String orderId,
    Map<String, dynamic> data,
  ) async {
    try {
      // Backend cancel endpoint: PUT /{orderId}/cancel
      final response = await ApiClient.put(
        Uri.parse('$baseUrl/$orderId/cancel'),
      );

      if (response.statusCode == 200) {
        return OrderResponse(
          success: true,
          message: _messageFromResponse(
            response,
            successFallback: 'Order cancelled successfully',
          ),
        );
      }
      if (response.statusCode == 401) {
        return OrderResponse(success: false, message: 'Authentication required.');
      }

      return OrderResponse(
        success: false,
        message: ApiException.fromResponse(response).message,
      );
    } catch (e) {
      debugPrint('Error updating order: $e');
      if (e is ApiException) {
        return OrderResponse(success: false, message: e.message);
      }
      return OrderResponse(success: false, message: 'Error updating order.');
    }
  }

  Future<ReceiptDownloadResult> downloadReceipt(String orderId) async {
    try {
      final response = await ApiClient.get(
        Uri.parse('$baseUrl/$orderId/receipt'),
      );

      if (response.statusCode != 200) {
        if (response.statusCode == 401) {
          throw ApiException('Authentication required. Please login again.', statusCode: 401);
        }
        throw ApiException(
          _messageFromBytes(
            response,
            fallbackMessage: 'Failed to download receipt.',
          ),
          statusCode: response.statusCode,
        );
      }

      final fileName = _fileNameFromHeaders(
        response.headers['content-disposition'],
        orderId,
      );

      final savedPath = await FilePicker.saveFile(
        dialogTitle: 'Save Receipt',
        fileName: fileName,
        type: FileType.custom,
        allowedExtensions: const ['pdf'],
        bytes: response.bodyBytes,
      );

      return ReceiptDownloadResult(
        fileName: fileName,
        savedPath: savedPath,
        didSave: savedPath != null,
      );
    } catch (e) {
      debugPrint('Error downloading receipt: $e');
      if (e is ApiException) rethrow;
      throw ApiException('Failed to download receipt. Please try again.');
    }
  }

  String _messageFromResponse(
    dynamic response, {
    required String successFallback,
  }) {
    if (response.body.isEmpty) {
      return successFallback;
    }

    try {
      final dynamic decoded = json.decode(response.body);
      if (decoded is Map<String, dynamic>) {
        return (decoded['message'] ?? decoded['error'] ?? successFallback)
            .toString();
      }
      if (decoded is String && decoded.trim().isNotEmpty) {
        return decoded.trim();
      }
    } catch (_) {
      final body = response.body.trim();
      if (body.isNotEmpty) {
        return body;
      }
    }

    return successFallback;
  }

  String _messageFromBytes(
    dynamic response, {
    required String fallbackMessage,
  }) {
    final body = utf8.decode(response.bodyBytes, allowMalformed: true).trim();
    if (body.isEmpty) {
      return fallbackMessage;
    }

    try {
      final dynamic decoded = json.decode(body);
      if (decoded is Map<String, dynamic>) {
        return (decoded['message'] ?? decoded['error'] ?? fallbackMessage)
            .toString();
      }
      if (decoded is String && decoded.trim().isNotEmpty) {
        return decoded.trim();
      }
    } catch (_) {
      if (!body.startsWith('<')) {
        return body;
      }
    }

    return fallbackMessage;
  }

  String _fileNameFromHeaders(String? contentDisposition, String orderId) {
    if (contentDisposition == null || contentDisposition.trim().isEmpty) {
      return 'receipt-$orderId.pdf';
    }

    final utf8Match = RegExp(
      r"filename\*=UTF-8''([^;]+)",
      caseSensitive: false,
    ).firstMatch(contentDisposition);
    if (utf8Match != null) {
      return Uri.decodeComponent(utf8Match.group(1)!);
    }

    final standardMatch = RegExp(
      r'filename="?([^"]+)"?',
      caseSensitive: false,
    ).firstMatch(contentDisposition);
    if (standardMatch != null) {
      return standardMatch.group(1)!;
    }

    return 'receipt-$orderId.pdf';
  }
}
