import 'package:flutter/material.dart';
import 'package:flutter_application/features/auth/data/auth_service.dart';
import 'package:flutter_application/core/navigation/main_nav.dart';

class AuthGuard {
  static Future<bool> checkLoginOrRedirect(BuildContext context, {String message = 'Please login to continue'}) async {
    final isLoggedIn = await AuthService().isLoggedIn();
    if (!isLoggedIn && context.mounted) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(message),
          behavior: SnackBarBehavior.floating,
          backgroundColor: Colors.deepPurple,
        ),
      );
      Navigator.of(context).pushAndRemoveUntil(
        MaterialPageRoute(builder: (context) => MainNav(initialIndex: 3)),
        (route) => false,
      );
      return false;
    }
    return isLoggedIn;
  }
}
