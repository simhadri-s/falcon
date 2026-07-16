import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_dotenv/flutter_dotenv.dart';
import 'package:firebase_core/firebase_core.dart';
import 'package:firebase_messaging/firebase_messaging.dart';
import 'core/services/notification_service.dart';
import 'app.dart';

/// Top-level background message handler.
/// MUST be a top-level function (not inside a class).
/// Called when the app is in the background or terminated.
@pragma('vm:entry-point')
Future<void> _firebaseMessagingBackgroundHandler(RemoteMessage message) async {
  // Firebase must be initialized before using any Firebase services
  await Firebase.initializeApp();
  debugPrint('FCM [Background]: ${message.messageId} — ${message.notification?.title}');
}

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();

  await SystemChrome.setPreferredOrientations([
    DeviceOrientation.portraitUp,
    DeviceOrientation.portraitDown,
  ]);

  // Register background handler BEFORE Firebase.initializeApp()
  FirebaseMessaging.onBackgroundMessage(_firebaseMessagingBackgroundHandler);

  try {
    await Firebase.initializeApp();
    await NotificationService().initialize();
  } catch (e) {
    debugPrint("Firebase initialization failed: $e");
    debugPrint("Notifications will be disabled until Firebase is configured.");
  }

  await dotenv.load(fileName: ".env");
  runApp(const MyApp());
}