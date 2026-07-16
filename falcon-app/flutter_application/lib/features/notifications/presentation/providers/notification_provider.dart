import 'package:flutter/material.dart';
import 'dart:async';
import 'package:shared_preferences/shared_preferences.dart';
import '../../data/notification_model.dart';
import '../../data/notification_api_service.dart';
import '../../../../core/services/notification_service.dart';

class NotificationProvider with ChangeNotifier {
  final NotificationApiService _apiService = NotificationApiService();
  
  List<AppNotification> _notifications = [];
  int _unreadCount = 0;
  bool _isLoading = false;
  int _currentPage = 1;
  int _totalPages = 1;
  StreamSubscription? _notificationSubscription;
  Set<String> _clearedNotificationIds = {};

  List<AppNotification> get notifications => _notifications;
  int get unreadCount => _unreadCount;
  bool get isLoading => _isLoading;

  NotificationProvider() {
    _notificationSubscription = NotificationService().foregroundMessages.listen((message) {
      fetchUnreadCount();
    });
    _loadClearedNotifications();
  }

  @override
  void dispose() {
    _notificationSubscription?.cancel();
    super.dispose();
  }

  Future<void> _loadClearedNotifications() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      final list = prefs.getStringList('cleared_notification_ids') ?? [];
      _clearedNotificationIds = list.toSet();
      notifyListeners();
    } catch (e) {
      debugPrint('Error loading cleared notifications: $e');
    }
  }

  Future<void> _saveClearedNotifications() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      await prefs.setStringList('cleared_notification_ids', _clearedNotificationIds.toList());
    } catch (e) {
      debugPrint('Error saving cleared notifications: $e');
    }
  }

  Future<void> fetchNotifications({bool refresh = false}) async {
    if (refresh) {
      _currentPage = 1;
      _notifications = [];
    }

    if (_isLoading || (_currentPage > _totalPages && !refresh)) return;

    _isLoading = true;
    notifyListeners();

    try {
      final result = await _apiService.fetchNotifications(page: _currentPage);
      final fetchedList = result['notifications'] as List<AppNotification>;
      
      // Filter out notifications that have been locally cleared/dismissed
      final filteredList = fetchedList.where((n) => !_clearedNotificationIds.contains(n.id)).toList();
      
      _notifications.addAll(filteredList);
      _totalPages = result['totalPages'];
      _currentPage++;
      
      await fetchUnreadCount();
    } catch (e) {
      debugPrint('Error fetching notifications: $e');
    } finally {
      _isLoading = false;
      notifyListeners();
    }
  }

  Future<void> fetchUnreadCount() async {
    try {
      _unreadCount = await _apiService.getUnreadCount();
      notifyListeners();
    } catch (e) {
      debugPrint('Error fetching unread count: $e');
    }
  }

  Future<void> markAsRead(String id) async {
    try {
      await _apiService.markAsRead(id);
      final index = _notifications.indexWhere((n) => n.id == id);
      if (index != -1) {
        _notifications[index] = AppNotification(
          id: _notifications[index].id,
          userId: _notifications[index].userId,
          title: _notifications[index].title,
          body: _notifications[index].body,
          imageUrl: _notifications[index].imageUrl,
          data: _notifications[index].data,
          read: true,
          createdAt: _notifications[index].createdAt,
        );
        if (_unreadCount > 0) _unreadCount--;
        notifyListeners();
      }
    } catch (e) {
      debugPrint('Error marking as read: $e');
    }
  }

  Future<void> clearNotification(String id) async {
    try {
      await _apiService.deleteNotification(id);
    } catch (e) {
      debugPrint('Error deleting notification from server: $e');
    }

    // Add to cleared set and save
    _clearedNotificationIds.add(id);
    await _saveClearedNotifications();

    // Remove from in-memory list
    final index = _notifications.indexWhere((n) => n.id == id);
    if (index != -1) {
      final wasUnread = !_notifications[index].read;
      _notifications.removeAt(index);
      if (wasUnread && _unreadCount > 0) _unreadCount--;
    }
    notifyListeners();
  }

  Future<void> clearAllPersonalNotifications() async {
    try {
      await _apiService.clearAllNotifications();
    } catch (e) {
      debugPrint('Error clearing personal notifications on server: $e');
    }

    final personalIds = _notifications
        .where((n) => n.isPersonal)
        .map((n) => n.id)
        .toList();

    _clearedNotificationIds.addAll(personalIds);
    await _saveClearedNotifications();

    _notifications.removeWhere((n) => n.isPersonal);
    await fetchUnreadCount();
  }

  Future<void> clearAllGeneralNotifications() async {
    // General notifications are broadcasts, dismissed purely locally
    final generalIds = _notifications
        .where((n) => !n.isPersonal)
        .map((n) => n.id)
        .toList();

    _clearedNotificationIds.addAll(generalIds);
    await _saveClearedNotifications();

    _notifications.removeWhere((n) => !n.isPersonal);
    await fetchUnreadCount();
  }
}
