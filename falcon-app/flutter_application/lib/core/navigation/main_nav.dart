import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../features/home/presentation/pages/home_page.dart';
import '../../features/profile/presentation/pages/profile_page.dart';
import '../../features/products/presentation/pages/products_page.dart';
import '../../features/cart/presentation/pages/cart_page.dart';
import '../../features/cart/presentation/providers/cart_provider.dart';

import 'dart:async';
import '../services/notification_service.dart';
import '../../features/notifications/presentation/pages/notification_page.dart';
import '../../features/orders/presentation/pages/order_details_page.dart';

class MainNav extends StatefulWidget {
  final int initialIndex;
  const MainNav({super.key, this.initialIndex = 0});

  @override
  State<MainNav> createState() => _MainNavState();
}

class _MainNavState extends State<MainNav> {
  late int _currentIndex;
  StreamSubscription? _notificationSubscription;
  List<bool>? _visitedPages;

  List<bool> get visitedPages {
    _visitedPages ??= List.generate(4, (index) => index == widget.initialIndex);
    return _visitedPages!;
  }

  @override
  void initState() {
    super.initState();
    _currentIndex = widget.initialIndex;
    
    // Listen for notification clicks
    _notificationSubscription = NotificationService().notificationClicks.listen((data) {
      _handleNotificationClick(data);
    });
  }

  void _handleNotificationClick(Map<String, dynamic> data) {
    if (data['type'] == 'ORDER_UPDATE' && data['orderId'] != null) {
      Navigator.push(
        context,
        MaterialPageRoute(
          builder: (context) => OrderDetailsPage(orderId: data['orderId']),
        ),
      );
    } else {
      // Default behavior: go to notifications page
      Navigator.push(
        context,
        MaterialPageRoute(builder: (context) => const NotificationPage()),
      );
    }
  }

  @override
  void dispose() {
    _notificationSubscription?.cancel();
    super.dispose();
  }

  final List<Widget> _pages = const [
    HomePage(),
    ProductsPage(),
    CartPage(),
    ProfilePage(),
  ];

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: IndexedStack(
        index: _currentIndex,
        children: List.generate(_pages.length, (index) {
          if (visitedPages[index]) {
            return _pages[index];
          } else {
            return const SizedBox.shrink();
          }
        }),
      ),

      bottomNavigationBar: Container(
        decoration: BoxDecoration(
          color: Colors.white,
          boxShadow: [
            BoxShadow(
              color: Colors.black.withValues(alpha: 0.08),
              blurRadius: 20,
              offset: const Offset(0, -4),
            ),
          ],
          borderRadius: const BorderRadius.vertical(top: Radius.circular(16)),
        ),
        child: ClipRRect(
          borderRadius: const BorderRadius.vertical(top: Radius.circular(16)),
          child: BottomNavigationBar(
            currentIndex: _currentIndex,
            selectedItemColor: const Color(0xFF4F46E5),
            unselectedItemColor: Colors.grey.shade400,
            type: BottomNavigationBarType.fixed,
            backgroundColor: Colors.white,
            elevation: 0,
            selectedLabelStyle: const TextStyle(
              fontWeight: FontWeight.w600,
              fontSize: 11,
            ),
            unselectedLabelStyle: const TextStyle(
              fontWeight: FontWeight.w400,
              fontSize: 11,
            ),
            onTap: (index) {
              setState(() {
                _currentIndex = index;
                visitedPages[index] = true;
              });
            },
            items: [
              const BottomNavigationBarItem(
                icon: Icon(Icons.home_outlined),
                activeIcon: Icon(Icons.home),
                label: "Home",
              ),
              const BottomNavigationBarItem(
                icon: Icon(Icons.inventory_2_outlined),
                activeIcon: Icon(Icons.inventory_2),
                label: "Products",
              ),
              BottomNavigationBarItem(
                icon: Consumer<CartProvider>(
                  builder: (context, cart, child) {
                    return Badge(
                      label: Text("${cart.totalQuantity}"),
                      isLabelVisible: cart.totalQuantity > 0,
                      backgroundColor: const Color(0xFFF97316),
                      child: const Icon(Icons.shopping_bag_outlined),
                    );
                  },
                ),
                activeIcon: Consumer<CartProvider>(
                  builder: (context, cart, child) {
                    return Badge(
                      label: Text("${cart.totalQuantity}"),
                      isLabelVisible: cart.totalQuantity > 0,
                      backgroundColor: const Color(0xFFF97316),
                      child: const Icon(Icons.shopping_bag),
                    );
                  },
                ),
                label: "Cart",
              ),
              const BottomNavigationBarItem(
                icon: Icon(Icons.person_outline),
                activeIcon: Icon(Icons.person),
                label: "Profile",
              ),
            ],
          ),
        ),
      ),
    );
  }
}