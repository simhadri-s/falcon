import 'dart:async';
import 'dart:convert';
import 'dart:io';
import 'package:firebase_core/firebase_core.dart';
import 'package:firebase_messaging/firebase_messaging.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter_local_notifications/flutter_local_notifications.dart';
import 'package:http/http.dart' as http;
import 'package:path_provider/path_provider.dart';
import 'package:shared_preferences/shared_preferences.dart';
import '../constants/api_constants.dart';
import 'package:flutter_application/core/network/api_client.dart';

class NotificationService {
  static final NotificationService _instance = NotificationService._internal();
  factory NotificationService() => _instance;
  NotificationService._internal();

  final FirebaseMessaging _fcm = FirebaseMessaging.instance;
  final FlutterLocalNotificationsPlugin _localNotifications = FlutterLocalNotificationsPlugin();

  // Stream controller to broadcast notification clicks to the UI
  final StreamController<Map<String, dynamic>> _notificationClickController = 
      StreamController<Map<String, dynamic>>.broadcast();

  // Stream controller for incoming foreground messages
  final StreamController<RemoteMessage> _foregroundMessageController = 
      StreamController<RemoteMessage>.broadcast();
  
  Stream<Map<String, dynamic>> get notificationClicks => _notificationClickController.stream;
  Stream<RemoteMessage> get foregroundMessages => _foregroundMessageController.stream;

  Future<void> initialize() async {
    // 1. Request permissions (especially for iOS)
    NotificationSettings settings = await _fcm.requestPermission(
      alert: true,
      badge: true,
      sound: true,
    );

    if (kDebugMode) {
      print('User granted permission: ${settings.authorizationStatus}');
    }

    // 2. Initialize local notifications for foreground alerts
    const AndroidInitializationSettings androidSettings = 
        AndroidInitializationSettings('@mipmap/ic_launcher');
    const DarwinInitializationSettings iosSettings = DarwinInitializationSettings();
    
    const InitializationSettings initSettings = InitializationSettings(
      android: androidSettings,
      iOS: iosSettings,
    );

    await _localNotifications.initialize(
      initSettings,
      onDidReceiveNotificationResponse: (NotificationResponse response) {
        if (response.payload != null) {
          final data = jsonDecode(response.payload!);
          _notificationClickController.add(Map<String, dynamic>.from(data));
        }
      },
    );

    // 3. Create Android notification channel
    const AndroidNotificationChannel channel = AndroidNotificationChannel(
      'high_importance_channel',
      'High Importance Notifications',
      description: 'This channel is used for important notifications.',
      importance: Importance.max,
      playSound: true,
      enableVibration: true,
    );

    await _localNotifications
        .resolvePlatformSpecificImplementation<AndroidFlutterLocalNotificationsPlugin>()
        ?.createNotificationChannel(channel);

    // 4. Listen for foreground messages
    FirebaseMessaging.onMessage.listen((RemoteMessage message) async {
      _foregroundMessageController.add(message);
      RemoteNotification? notification = message.notification;

      if (notification != null && !kIsWeb) {
        String? imageUrl = notification.android?.imageUrl ?? notification.apple?.imageUrl;
        
        BigPictureStyleInformation? bigPictureStyleInformation;
        if (imageUrl != null && imageUrl.isNotEmpty) {
          try {
            final String largeIconPath = await _downloadAndSaveFile(imageUrl, 'largeIcon');
            final String bigPicturePath = await _downloadAndSaveFile(imageUrl, 'bigPicture');
            
            bigPictureStyleInformation = BigPictureStyleInformation(
              FilePathAndroidBitmap(bigPicturePath),
              largeIcon: FilePathAndroidBitmap(largeIconPath),
              contentTitle: notification.title,
              summaryText: notification.body,
            );
          } catch (e) {
            debugPrint('Error downloading notification image: $e');
          }
        }

        _localNotifications.show(
          notification.hashCode,
          notification.title,
          notification.body,
          NotificationDetails(
            android: AndroidNotificationDetails(
              channel.id,
              channel.name,
              channelDescription: channel.description,
              importance: Importance.max,
              priority: Priority.high,
              playSound: true,
              enableVibration: true,
              icon: '@mipmap/ic_launcher',
              styleInformation: bigPictureStyleInformation,
            ),
            iOS: DarwinNotificationDetails(
              attachments: imageUrl != null ? [DarwinNotificationAttachment(await _downloadAndSaveFile(imageUrl, 'notification_img'))] : null,
            ),
          ),
          payload: jsonEncode(message.data),
        );
      }
    });

    // 5. Handle notification click when app is in background/terminated
    FirebaseMessaging.onMessageOpenedApp.listen((RemoteMessage message) {
      _notificationClickController.add(message.data);
    });

    // Check if app was opened from a terminated state via a notification
    RemoteMessage? initialMessage = await _fcm.getInitialMessage();
    if (initialMessage != null) {
      _notificationClickController.add(initialMessage.data);
    }

    // 6. Auto-register token whenever Firebase refreshes it
    _fcm.onTokenRefresh.listen((newToken) {
      debugPrint('FCM: Token refreshed, re-registering...');
      _sendTokenToBackend(newToken);
    });
  }

  Future<String?> getToken() async {
    return await _fcm.getToken();
  }

  /// Call this after user logs in to ensure their FCM token is registered.
  /// Retries a few times in case Firebase hasn't generated the token yet.
  Future<void> autoRegisterToken(String authToken) async {
    for (int attempt = 0; attempt < 3; attempt++) {
      final fcmToken = await _fcm.getToken();
      if (fcmToken != null) {
        await _sendTokenToBackend(fcmToken);
        return;
      }
      await Future.delayed(const Duration(seconds: 2));
    }
    debugPrint('FCM: Could not obtain token after 3 attempts.');
  }

  Future<void> _sendTokenToBackend(String fcmToken) async {
    try {
      final token = await ApiClient.getToken();
      if (token == null || token.isEmpty) {
        debugPrint('FCM: No auth token found, skipping registration.');
        return;
      }

      final response = await ApiClient.post(
        Uri.parse('${ApiConstants.baseUrl}/users/fcm-token'),
        body: jsonEncode({'token': fcmToken}),
      );

      if (response.statusCode == 200 || response.statusCode == 201) {
        debugPrint('FCM: Token registered successfully.');
      } else {
        debugPrint('FCM: Failed to register token. Status: ${response.statusCode} Body: ${response.body}');
      }
    } catch (e) {
      debugPrint('FCM: Error sending token to backend: $e');
    }
  }

  Stream<String> get onTokenRefresh => _fcm.onTokenRefresh;

  Future<void> removeFcmToken(String fcmToken) async {
    try {
      final token = await ApiClient.getToken();
      if (token == null) return;

      await ApiClient.delete(
        Uri.parse('${ApiConstants.baseUrl}/users/fcm-token'),
        body: jsonEncode({'token': fcmToken}),
      );
      debugPrint('FCM: Token removed from backend.');
    } catch (e) {
      debugPrint('FCM: Error removing token: $e');
    }
  }

  Future<String> _downloadAndSaveFile(String url, String fileName) async {
    final Directory directory = await getTemporaryDirectory();
    final String filePath = '${directory.path}/$fileName';
    final http.Response response = await http.get(Uri.parse(url));
    final File file = File(filePath);
    await file.writeAsBytes(response.bodyBytes);
    return filePath;
  }
}
