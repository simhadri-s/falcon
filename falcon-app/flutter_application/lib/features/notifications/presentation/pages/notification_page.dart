import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:intl/intl.dart';
import '../providers/notification_provider.dart';
import '../../data/notification_model.dart';
import '../../../orders/presentation/pages/order_details_page.dart';

class NotificationPage extends StatefulWidget {
  const NotificationPage({super.key});

  @override
  State<NotificationPage> createState() => _NotificationPageState();
}

class _NotificationPageState extends State<NotificationPage> {
  final ScrollController _personalScrollController = ScrollController();
  final ScrollController _generalScrollController = ScrollController();

  @override
  void initState() {
    super.initState();
    Future.microtask(() {
      if (!mounted) return;
      Provider.of<NotificationProvider>(context, listen: false).fetchNotifications(refresh: true);
    });

    _personalScrollController.addListener(() {
      if (_personalScrollController.position.pixels >= _personalScrollController.position.maxScrollExtent - 200) {
        Provider.of<NotificationProvider>(context, listen: false).fetchNotifications();
      }
    });

    _generalScrollController.addListener(() {
      if (_generalScrollController.position.pixels >= _generalScrollController.position.maxScrollExtent - 200) {
        Provider.of<NotificationProvider>(context, listen: false).fetchNotifications();
      }
    });
  }

  @override
  void dispose() {
    _personalScrollController.dispose();
    _generalScrollController.dispose();
    super.dispose();
  }

  void _handleNotificationClick(AppNotification notification) {
    if (!notification.read) {
      Provider.of<NotificationProvider>(context, listen: false).markAsRead(notification.id);
    }

    if (notification.data['type'] == 'ORDER_UPDATE' && notification.data['orderId'] != null) {
      Navigator.push(
        context,
        MaterialPageRoute(
          builder: (context) => OrderDetailsPage(orderId: notification.data['orderId']),
        ),
      );
    }
  }

  void _showClearAllDialog(BuildContext context) {
    final tabController = DefaultTabController.of(context);
    final isPersonalTab = tabController.index == 0;
    final activeTabName = isPersonalTab ? 'Personal' : 'General';
    final provider = Provider.of<NotificationProvider>(context, listen: false);

    // If there are no notifications in this tab, don't show clear dialog
    final tabNotifications = provider.notifications.where((n) => isPersonalTab ? n.isPersonal : !n.isPersonal).toList();
    if (tabNotifications.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('No $activeTabName notifications to clear')),
      );
      return;
    }

    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        title: Text('Clear All $activeTabName?'),
        content: Text('Are you sure you want to clear all $activeTabName notifications? This action cannot be undone.'),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(ctx),
            child: const Text('Cancel', style: TextStyle(color: Colors.grey)),
          ),
          ElevatedButton(
            style: ElevatedButton.styleFrom(
              backgroundColor: const Color(0xFFF97316),
              foregroundColor: Colors.white,
              elevation: 0,
              padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
              shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
            ),
            onPressed: () {
              Navigator.pop(ctx);
              if (isPersonalTab) {
                provider.clearAllPersonalNotifications();
              } else {
                provider.clearAllGeneralNotifications();
              }
              ScaffoldMessenger.of(context).showSnackBar(
                SnackBar(content: Text('Cleared all $activeTabName notifications')),
              );
            },
            child: const Text('Clear All'),
          ),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Consumer<NotificationProvider>(
      builder: (context, provider, child) {
        // Segregate notifications
        final personalNotifications = provider.notifications.where((n) => n.isPersonal).toList();
        final generalNotifications = provider.notifications.where((n) => !n.isPersonal).toList();

        // Count unread notifications for each category
        final unreadPersonalCount = personalNotifications.where((n) => !n.read).length;
        final unreadGeneralCount = generalNotifications.where((n) => !n.read).length;

        return DefaultTabController(
          length: 2,
          child: Scaffold(
            backgroundColor: Colors.grey[50],
            appBar: AppBar(
              elevation: 0,
              backgroundColor: Colors.white,
              foregroundColor: Colors.black87,
              title: const Text(
                'Notifications',
                style: TextStyle(
                  fontWeight: FontWeight.w700,
                  fontSize: 20,
                  letterSpacing: -0.5,
                ),
              ),
              actions: [
                Builder(
                  builder: (context) {
                    return IconButton(
                      icon: const Icon(Icons.delete_sweep_outlined, color: Colors.black87),
                      tooltip: 'Clear All',
                      onPressed: () => _showClearAllDialog(context),
                    );
                  },
                ),
              ],
              bottom: TabBar(
                indicatorColor: const Color(0xFF4F46E5),
                indicatorWeight: 3,
                labelColor: const Color(0xFF4F46E5),
                unselectedLabelColor: Colors.grey[500],
                labelStyle: const TextStyle(fontWeight: FontWeight.w700, fontSize: 14.5),
                unselectedLabelStyle: const TextStyle(fontWeight: FontWeight.w600, fontSize: 14.5),
                indicatorSize: TabBarIndicatorSize.tab,
                tabs: [
                  Tab(
                    child: Row(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        const Text('Personal'),
                        if (unreadPersonalCount > 0) ...[
                          const SizedBox(width: 8),
                          Container(
                            padding: const EdgeInsets.symmetric(horizontal: 7, vertical: 2),
                            decoration: BoxDecoration(
                              color: const Color(0xFFF97316),
                              borderRadius: BorderRadius.circular(10),
                            ),
                            child: Text(
                              '$unreadPersonalCount',
                              style: const TextStyle(
                                color: Colors.white,
                                fontSize: 10,
                                fontWeight: FontWeight.bold,
                              ),
                            ),
                          ),
                        ],
                      ],
                    ),
                  ),
                  Tab(
                    child: Row(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        const Text('General'),
                        if (unreadGeneralCount > 0) ...[
                          const SizedBox(width: 8),
                          Container(
                            padding: const EdgeInsets.symmetric(horizontal: 7, vertical: 2),
                            decoration: BoxDecoration(
                              color: const Color(0xFFF97316),
                              borderRadius: BorderRadius.circular(10),
                            ),
                            child: Text(
                              '$unreadGeneralCount',
                              style: const TextStyle(
                                color: Colors.white,
                                fontSize: 10,
                                fontWeight: FontWeight.bold,
                              ),
                            ),
                          ),
                        ],
                      ],
                    ),
                  ),
                ],
              ),
            ),
            body: TabBarView(
              children: [
                _buildNotificationList(
                  context,
                  provider,
                  personalNotifications,
                  _personalScrollController,
                  'No personal notifications yet',
                ),
                _buildNotificationList(
                  context,
                  provider,
                  generalNotifications,
                  _generalScrollController,
                  'No general announcements yet',
                ),
              ],
            ),
          ),
        );
      },
    );
  }

  Widget _buildNotificationList(
    BuildContext context,
    NotificationProvider provider,
    List<AppNotification> notifications,
    ScrollController scrollController,
    String emptyMessage,
  ) {
    if (provider.isLoading && notifications.isEmpty) {
      return const Center(child: CircularProgressIndicator());
    }

    if (notifications.isEmpty) {
      return Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(Icons.notifications_off_outlined, size: 64, color: Colors.grey[300]),
            const SizedBox(height: 16),
            Text(
              emptyMessage,
              style: TextStyle(
                color: Colors.grey[500],
                fontSize: 16,
                fontWeight: FontWeight.w600,
              ),
            ),
          ],
        ),
      );
    }

    return RefreshIndicator(
      onRefresh: () => provider.fetchNotifications(refresh: true),
      child: ListView.separated(
        physics: const AlwaysScrollableScrollPhysics(),
        controller: scrollController,
        padding: const EdgeInsets.symmetric(vertical: 8),
        itemCount: notifications.length + (provider.isLoading ? 1 : 0),
        separatorBuilder: (context, index) => const Divider(height: 1, indent: 72),
        itemBuilder: (context, index) {
          if (index == notifications.length) {
            return const Padding(
              padding: EdgeInsets.all(16.0),
              child: Center(child: CircularProgressIndicator()),
            );
          }

          final notification = notifications[index];
          final type = notification.data['type'];
          final isRead = notification.read;

          return Dismissible(
            key: Key(notification.id),
            direction: DismissDirection.endToStart,
            background: Container(
              color: Colors.red[600],
              alignment: Alignment.centerRight,
              padding: const EdgeInsets.symmetric(horizontal: 20),
              child: const Row(
                mainAxisAlignment: MainAxisAlignment.end,
                children: [
                  Text(
                    'Clear',
                    style: TextStyle(
                      color: Colors.white,
                      fontWeight: FontWeight.w600,
                      fontSize: 14,
                    ),
                  ),
                  SizedBox(width: 8),
                  Icon(
                    Icons.delete_outline_rounded,
                    color: Colors.white,
                    size: 24,
                  ),
                ],
              ),
            ),
            onDismissed: (direction) {
              provider.clearNotification(notification.id);
              ScaffoldMessenger.of(context).showSnackBar(
                const SnackBar(
                  content: Text('Notification cleared'),
                  duration: Duration(seconds: 2),
                ),
              );
            },
            child: Container(
              color: isRead ? Colors.transparent : const Color(0xFF4F46E5).withValues(alpha: 0.03),
              child: ListTile(
                onTap: () => _handleNotificationClick(notification),
                contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
                leading: CircleAvatar(
                  radius: 22,
                  backgroundColor: isRead ? Colors.grey[100] : const Color(0xFF4F46E5).withValues(alpha: 0.1),
                  child: Icon(
                    _getIconForType(type),
                    color: isRead ? Colors.grey[500] : const Color(0xFF4F46E5),
                    size: 22,
                  ),
                ),
                title: Text(
                  notification.title,
                  style: TextStyle(
                    fontWeight: isRead ? FontWeight.w500 : FontWeight.w700,
                    fontSize: 14.5,
                    color: isRead ? Colors.grey[800] : Colors.black87,
                  ),
                ),
                subtitle: Padding(
                  padding: const EdgeInsets.only(top: 4.0),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        notification.body,
                        style: TextStyle(
                          fontSize: 13,
                          color: Colors.grey[600],
                          height: 1.3,
                        ),
                      ),
                      const SizedBox(height: 6),
                      Text(
                        DateFormat('MMM d, h:mm a').format(notification.createdAt),
                        style: TextStyle(fontSize: 11, color: Colors.grey[400]),
                      ),
                    ],
                  ),
                ),
                trailing: Row(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    if (!isRead) ...[
                      const Icon(Icons.circle, size: 8, color: Color(0xFFF97316)),
                      const SizedBox(width: 8),
                    ],
                    IconButton(
                      icon: Icon(Icons.clear_rounded, size: 20, color: Colors.grey[400]),
                      padding: EdgeInsets.zero,
                      constraints: const BoxConstraints(),
                      splashRadius: 18,
                      tooltip: 'Clear notification',
                      onPressed: () {
                        provider.clearNotification(notification.id);
                        ScaffoldMessenger.of(context).showSnackBar(
                          const SnackBar(
                            content: Text('Notification cleared'),
                            duration: Duration(seconds: 2),
                          ),
                        );
                      },
                    ),
                  ],
                ),
              ),
            ),
          );
        },
      ),
    );
  }

  IconData _getIconForType(String? type) {
    switch (type) {
      case 'ORDER_UPDATE':
        return Icons.shopping_bag_outlined;
      case 'OFFER':
        return Icons.local_offer_outlined;
      case 'ANNOUNCEMENT':
        return Icons.campaign_outlined;
      default:
        return Icons.notifications_outlined;
    }
  }
}
