import 'dart:convert';
import 'package:http/http.dart' as http;
import 'package:shared_preferences/shared_preferences.dart';
import 'package:flutter_application/core/error/api_exception.dart';

class ApiClient {
  static const String _tokenKey = 'auth_token';

  static Future<String?> getToken() async {
    final prefs = await SharedPreferences.getInstance();
    return prefs.getString(_tokenKey);
  }

  static Future<Map<String, String>> _getHeaders({Map<String, String>? customHeaders}) async {
    final headers = {
      'Content-Type': 'application/json',
      ...?customHeaders,
    };

    final token = await getToken();
    if (token != null && token.isNotEmpty) {
      headers['Authorization'] = 'Bearer $token';
    }

    return headers;
  }

  static Future<http.Response> get(Uri url, {Map<String, String>? headers}) async {
    final finalHeaders = await _getHeaders(customHeaders: headers);
    final response = await http.get(url, headers: finalHeaders);
    _checkUnauthorized(response);
    return response;
  }

  static Future<http.Response> post(Uri url, {Map<String, String>? headers, Object? body, Encoding? encoding}) async {
    final finalHeaders = await _getHeaders(customHeaders: headers);
    final response = await http.post(url, headers: finalHeaders, body: body, encoding: encoding);
    _checkUnauthorized(response);
    return response;
  }

  static Future<http.Response> put(Uri url, {Map<String, String>? headers, Object? body, Encoding? encoding}) async {
    final finalHeaders = await _getHeaders(customHeaders: headers);
    final response = await http.put(url, headers: finalHeaders, body: body, encoding: encoding);
    _checkUnauthorized(response);
    return response;
  }

  static Future<http.Response> delete(Uri url, {Map<String, String>? headers, Object? body, Encoding? encoding}) async {
    final finalHeaders = await _getHeaders(customHeaders: headers);
    final response = await http.delete(url, headers: finalHeaders, body: body, encoding: encoding);
    _checkUnauthorized(response);
    return response;
  }

  static Future<http.Response> patch(Uri url, {Map<String, String>? headers, Object? body, Encoding? encoding}) async {
    final finalHeaders = await _getHeaders(customHeaders: headers);
    final response = await http.patch(url, headers: finalHeaders, body: body, encoding: encoding);
    _checkUnauthorized(response);
    return response;
  }

  static void _checkUnauthorized(http.Response response) {
    if (response.statusCode == 401) {
      // Optional: Handle global logout here by dispatching an event or clearing SharedPreferences
      // For now, we just let the ApiException handle it naturally where it's caught
    }
  }
}
