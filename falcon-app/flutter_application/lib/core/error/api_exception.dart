import 'dart:convert';
import 'package:http/http.dart' as http;

class ApiException implements Exception {
  final String message;
  final int? statusCode;
  final String? error;
  final String? path;

  ApiException(this.message, {this.statusCode, this.error, this.path});

  @override
  String toString() {
    return message;
  }

  factory ApiException.fromResponse(http.Response response) {
    String? errorMsg;
    String? error;
    String? path;

    try {
      final dynamic body = json.decode(response.body);
      if (body is Map<String, dynamic>) {
        errorMsg =
            _asString(body['message']) ??
            _asString(body['error']) ??
            _asString(body['detail']);
        error = _asString(body['error']);
        path = _asString(body['path']);
      } else if (body is String && body.trim().isNotEmpty) {
        errorMsg = body.trim();
      }
    } catch (_) {
      final rawBody = response.body.trim();
      if (rawBody.isNotEmpty && !rawBody.startsWith('<')) {
        errorMsg = rawBody;
      }
    }

    errorMsg ??= response.reasonPhrase;
    errorMsg ??= _fallbackMessage(response.statusCode);

    return ApiException(
      errorMsg,
      statusCode: response.statusCode,
      error: error,
      path: path,
    );
  }

  static String? _asString(dynamic value) {
    if (value == null) {
      return null;
    }

    final text = value.toString().trim();
    return text.isEmpty ? null : text;
  }

  static String _fallbackMessage(int? statusCode) {
    switch (statusCode) {
      case 400:
        return 'Invalid request. Please check your input and try again.';
      case 401:
        return 'Authentication required. Please login again.';
      case 403:
        return 'You do not have permission to perform this action.';
      case 404:
        return 'The requested resource was not found.';
      case 409:
        return 'This action could not be completed because of a conflict.';
      case 500:
        return 'Something went wrong on the server. Please try again.';
      default:
        return 'An unexpected error occurred.';
    }
  }
}
