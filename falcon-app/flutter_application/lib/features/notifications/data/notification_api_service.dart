import 'dart:convert';
import 'package:flutter/foundation.dart';
import 'package:flutter_application/core/network/api_client.dart';
import 'package:flutter_application/core/constants/api_constants.dart';
import 'notification_model.dart';

class NotificationApiService {
  static String get baseUrl => ApiConstants.baseUrl;

  Future<Map<String, dynamic>> fetchNotifications({int page = 1, int limit = 20}) async {
    final token = await ApiClient.getToken();
    if (token == null || token.isEmpty) {
      debugPrint('NotificationApiService: No auth token found');
      return {'notifications': [], 'totalPages': 0, 'totalElements': 0};
    }

    final response = await ApiClient.get(
      Uri.parse('$baseUrl/notifications?page=$page&limit=$limit'),
    );

    if (response.statusCode == 200) {
      final data = jsonDecode(response.body);
      final List<dynamic> content = data['content'] ?? [];
      return {
        'notifications': content.map((json) => AppNotification.fromJson(json)).toList(),
        'totalPages': data['totalPages'] ?? 1,
        'totalElements': data['totalElements'] ?? 0,
      };
    } else {
      throw Exception('Failed to load notifications');
    }
  }

  Future<int> getUnreadCount() async {
    final token = await ApiClient.getToken();
    if (token == null || token.isEmpty) return 0;

    final response = await ApiClient.get(
      Uri.parse('$baseUrl/notifications/unread-count'),
    );

    if (response.statusCode == 200) {
      final data = jsonDecode(response.body);
      return data['unreadCount'] ?? 0;
    }
    return 0;
  }

  Future<void> markAsRead(String id) async {
    final token = await ApiClient.getToken();
    if (token == null || token.isEmpty) return;

    await ApiClient.put(
      Uri.parse('$baseUrl/notifications/$id/read'),
    );
  }

  Future<void> deleteNotification(String id) async {
    final token = await ApiClient.getToken();
    if (token == null || token.isEmpty) return;

    final response = await ApiClient.delete(
      Uri.parse('$baseUrl/notifications/$id'),
    );

    if (response.statusCode != 200 && response.statusCode != 204) {
      throw Exception('Failed to delete notification');
    }
  }

  Future<void> clearAllNotifications() async {
    final token = await ApiClient.getToken();
    if (token == null || token.isEmpty) return;

    final response = await ApiClient.delete(
      Uri.parse('$baseUrl/notifications/clear-all'),
    );

    if (response.statusCode != 200 && response.statusCode != 204) {
      throw Exception('Failed to clear notifications');
    }
  }

  Future<void> registerFcmToken(String fcmToken) async {
    final token = await ApiClient.getToken();
    if (token == null || token.isEmpty) return;

    await ApiClient.post(
      Uri.parse('$baseUrl/users/fcm-token'),
      body: jsonEncode({'token': fcmToken}),
    );
  }

  Future<void> unregisterFcmToken(String fcmToken) async {
    final token = await ApiClient.getToken();
    if (token == null || token.isEmpty) return;

    await ApiClient.delete(
      Uri.parse('$baseUrl/users/fcm-token'),
      body: jsonEncode({'token': fcmToken}),
    );
  }
}
