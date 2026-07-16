import 'dart:convert';
import 'package:flutter/foundation.dart';
import 'package:flutter_application/core/network/api_client.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:flutter_application/core/error/api_exception.dart';
import 'package:flutter_application/core/constants/api_constants.dart';

class User {
  final String name;
  final String email;

  User({required this.name, required this.email});

  Map<String, dynamic> toJson() => {'name': name, 'email': email};

  factory User.fromJson(Map<String, dynamic> json) {
    return User(name: json['name'] ?? '', email: json['email'] ?? '');
  }
}

class AuthService {
  static String get baseUrl => '${ApiConstants.baseUrl}/auth';
  static const String _tokenKey = 'auth_token';
  static const String _userKey = 'user_data';

  Future<bool> login(String email, String password) async {
    try {
      final response = await ApiClient.post(
        Uri.parse('$baseUrl/login'),
        body: json.encode({'email': email, 'password': password}),
      );

      if (response.statusCode == 200 || response.statusCode == 201) {
        final Map<String, dynamic> body = json.decode(response.body);

        String? token;
        Map<String, dynamic>? userData;

        if (body.containsKey('token')) {
          token = body['token'];
        } else if (body.containsKey('data') && body['data'] is Map) {
          token = body['data']['token'];
          userData = body['data']['user'];
        }

        if (token != null) {
          final prefs = await SharedPreferences.getInstance();
          await prefs.setString(_tokenKey, token);

          if (userData != null) {
            await prefs.setString(_userKey, json.encode(userData));
          } else {
            // Mock user data if not provided by API
            await prefs.setString(
              _userKey,
              json.encode({'name': 'Falcon User', 'email': email}),
            );
          }
          return true;
        }
      }
      throw ApiException.fromResponse(response);
    } catch (e) {
      if (e is ApiException) rethrow;
      throw ApiException(
        'Network error occurred. Please check your connection.',
      );
    }
  }

  Future<bool> register(String name, String email, String password) async {
    try {
      final response = await ApiClient.post(
        Uri.parse('$baseUrl/register'),
        body: json.encode({'name': name, 'email': email, 'password': password}),
      );

      if (response.statusCode == 200 || response.statusCode == 201) {
        return true;
      }
      throw ApiException.fromResponse(response);
    } catch (e) {
      if (e is ApiException) rethrow;
      throw ApiException(
        'Network error occurred. Please check your connection.',
      );
    }
  }

  Future<void> logout() async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.remove(_tokenKey);
    await prefs.remove(_userKey);
  }

  Future<bool> isLoggedIn() async {
    final prefs = await SharedPreferences.getInstance();
    return prefs.containsKey(_tokenKey);
  }

  Future<String?> getToken() async {
    final prefs = await SharedPreferences.getInstance();
    return prefs.getString(_tokenKey);
  }

  Future<User?> getUserProfile() async {
    try {
      final token = await getToken();
      if (token == null) return null;

      final response = await ApiClient.get(
        Uri.parse(
          '${ApiConstants.baseUrl}/users/me',
        ), 
      );

      if (response.statusCode == 200) {
        final body = json.decode(response.body);
        final userData = body['data'] ?? body;
        final user = User.fromJson(userData);

        final prefs = await SharedPreferences.getInstance();
        await prefs.setString(_userKey, json.encode(user.toJson()));
        return user;
      }

      // Fallback to local storage if API fails
      final prefs = await SharedPreferences.getInstance();
      final userString = prefs.getString(_userKey);
      if (userString != null) {
        return User.fromJson(json.decode(userString));
      }
    } catch (e) {
      debugPrint('Error fetching profile: $e');
    }
    return null;
  }

  Future<bool> updateUserProfile(
    String name,
    String email,
    String? password,
  ) async {
    try {
      final token = await getToken();
      if (token == null) {
        throw ApiException(
          'Authentication required. Please login again.',
          statusCode: 401,
        );
      }

      final response = await ApiClient.put(
        Uri.parse('$baseUrl/update'),
        body: json.encode({'name': name, 'email': email, 'password': password}),
      );

      if (response.statusCode == 200) {
        final prefs = await SharedPreferences.getInstance();
        final user = User(name: name, email: email);
        await prefs.setString(_userKey, json.encode(user.toJson()));
        return true;
      } else {
        throw ApiException.fromResponse(response);
      }
    } catch (e) {
      if (e is ApiException) rethrow;
      throw ApiException(
        'Network error occurred. Please check your connection.',
      );
    }
  }

  Future<bool> forgotPassword(String email) async {
    try {
      final response = await ApiClient.post(
        Uri.parse('$baseUrl/forgot-password'),
        body: json.encode({'email': email}),
      );

      if (response.statusCode == 200) {
        return true;
      }
      throw ApiException.fromResponse(response);
    } catch (e) {
      if (e is ApiException) rethrow;
      throw ApiException(
        'Network error occurred. Please check your connection.',
      );
    }
  }

  Future<void> saveToken(String token) async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString(_tokenKey, token);
    await getUserProfile();
  }

  Future<bool> resetPassword(
    String email,
    String otp,
    String newPassword,
  ) async {
    try {
      final response = await ApiClient.post(
        Uri.parse('$baseUrl/reset-password'),
        body: json.encode({
          'email': email,
          'otp': otp,
          'password': newPassword,
        }),
      );

      if (response.statusCode == 200) {
        return true;
      }
      throw ApiException.fromResponse(response);
    } catch (e) {
      if (e is ApiException) rethrow;
      throw ApiException(
        'Network error occurred. Please check your connection.',
      );
    }
  }
}
