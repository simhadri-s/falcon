import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'core/theme/app_theme.dart';
import 'features/cart/presentation/providers/cart_provider.dart';
import 'features/company/presentation/providers/company_provider.dart';
import 'features/wishlist/presentation/providers/wishlist_provider.dart';
import 'features/notifications/presentation/providers/notification_provider.dart';
import 'features/home/presentation/pages/splash_screen.dart';

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  static final GlobalKey<NavigatorState> navigatorKey = GlobalKey<NavigatorState>();

  @override
  Widget build(BuildContext context) {
    return MultiProvider(
      providers: [
        ChangeNotifierProvider(create: (_) => CartProvider()),
        ChangeNotifierProvider(create: (_) => CompanyProvider()),
        ChangeNotifierProvider(create: (_) => WishlistProvider()..fetchWishlist()),
        ChangeNotifierProvider(create: (_) => NotificationProvider()..fetchUnreadCount()),
      ],
      child: MaterialApp(
        navigatorKey: navigatorKey,
        debugShowCheckedModeBanner: false,
        theme: AppTheme.lightTheme,
        home: const SplashScreen(),
      ),
    );
  }
}