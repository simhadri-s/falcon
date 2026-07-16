import 'package:flutter/material.dart';
import '../error/api_exception.dart';

class ErrorHandler {
  static String messageFor(dynamic error) {
    if (error is ApiException) {
      return error.message;
    }

    if (error is Exception) {
      return error.toString().replaceFirst('Exception: ', '');
    }

    if (error is String) {
      return error;
    }

    return 'An unexpected error occurred. Please try again.';
  }

  static void showError(BuildContext context, dynamic error) {
    final message = messageFor(error);
    final messenger = ScaffoldMessenger.of(context);

    messenger
      ..hideCurrentSnackBar()
      ..showSnackBar(
        SnackBar(
          content: Row(
            children: [
              const Icon(Icons.error_outline, color: Colors.white),
              const SizedBox(width: 12),
              Expanded(
                child: Text(
                  message,
                  style: const TextStyle(
                    color: Colors.white,
                    fontWeight: FontWeight.w500,
                    fontSize: 14,
                  ),
                ),
              ),
            ],
          ),
          backgroundColor: Colors.redAccent.shade700,
          behavior: SnackBarBehavior.floating,
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(12),
          ),
          margin: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
          duration: const Duration(seconds: 4),
          elevation: 4,
        ),
      );
  }
}
