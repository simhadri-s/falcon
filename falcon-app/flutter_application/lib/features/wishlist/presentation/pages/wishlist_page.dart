import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:flutter_application/features/wishlist/presentation/providers/wishlist_provider.dart';
import 'package:flutter_application/features/cart/presentation/providers/cart_provider.dart';
import 'package:flutter_application/features/home/data/product_model.dart';
import 'package:flutter_application/features/products/presentation/widgets/product_grid_item.dart';
import 'package:flutter_application/core/utils/error_handler.dart';
import 'package:flutter_application/core/utils/auth_guard.dart';

class WishlistPage extends StatelessWidget {
  const WishlistPage({super.key});

  @override
  Widget build(BuildContext context) {
    return Consumer2<WishlistProvider, CartProvider>(
      builder: (context, wishlistProvider, cartProvider, child) {
        final bool showBottomButton = !wishlistProvider.isLoading &&
            wishlistProvider.wishlistedProducts.isNotEmpty;

        return Scaffold(
          backgroundColor: Colors.grey.shade50,
          appBar: AppBar(
            title: const Text(
              "My Wishlist",
              style: TextStyle(fontWeight: FontWeight.bold),
            ),
            backgroundColor: Colors.white,
            foregroundColor: Colors.black,
            elevation: 0,
            centerTitle: true,
          ),
          body: () {
            if (wishlistProvider.isLoading) {
              return const Center(child: CircularProgressIndicator());
            }

            if (wishlistProvider.wishlistedProducts.isEmpty) {
              return const Center(
                child: Column(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    Icon(Icons.favorite_border, size: 80, color: Colors.grey),
                    SizedBox(height: 16),
                    Text(
                      "Your wishlist is empty",
                      style: TextStyle(fontSize: 18, color: Colors.grey),
                    ),
                  ],
                ),
              );
            }

            return RefreshIndicator(
              onRefresh: wishlistProvider.fetchWishlist,
              child: GridView.builder(
                padding: const EdgeInsets.all(16),
                physics: const AlwaysScrollableScrollPhysics(),
                gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
                  crossAxisCount: 2,
                  childAspectRatio: 0.65,
                  crossAxisSpacing: 12,
                  mainAxisSpacing: 16,
                ),
                itemCount: wishlistProvider.wishlistedProducts.length,
                itemBuilder: (context, index) {
                  final product = wishlistProvider.wishlistedProducts[index];
                  return ProductGridItem(
                    product: product,
                    heroTag: 'wishlist-${product.id}-$index',
                    onCartTap: () async {
                      final loggedIn = await AuthGuard.checkLoginOrRedirect(
                        context,
                        message: 'Please login to add items to cart',
                      );
                      if (!context.mounted) return;
                      if (!loggedIn) return;

                      // Show simple indicator
                      showDialog(
                        context: context,
                        barrierDismissible: false,
                        builder: (context) => const Center(
                          child: CircularProgressIndicator(),
                        ),
                      );

                      try {
                        // 1. Add to Cart
                        await cartProvider.addItem(product);
                        // 2. Remove from Wishlist
                        await wishlistProvider.toggleWishlist(product.id);

                        if (context.mounted) {
                          Navigator.pop(context); // Close loading indicator
                          ScaffoldMessenger.of(context).showSnackBar(
                            SnackBar(
                              content: Text('${product.name} moved to cart'),
                              backgroundColor: const Color(0xFF4F46E5),
                              duration: const Duration(seconds: 2),
                              behavior: SnackBarBehavior.floating,
                              shape: RoundedRectangleBorder(
                                borderRadius: BorderRadius.circular(8),
                              ),
                            ),
                          );
                        }
                      } catch (e) {
                        if (context.mounted) {
                          Navigator.pop(context); // Close loading indicator
                          ErrorHandler.showError(context, e);
                        }
                      }
                    },
                  );
                },
              ),
            );
          }(),
          bottomNavigationBar: showBottomButton
              ? Container(
                  padding: const EdgeInsets.all(16),
                  decoration: BoxDecoration(
                    color: Colors.white,
                    boxShadow: [
                      BoxShadow(
                        color: Colors.black.withOpacity(0.05),
                        blurRadius: 10,
                        offset: const Offset(0, -4),
                      ),
                    ],
                  ),
                  child: SafeArea(
                    child: ElevatedButton(
                      style: ElevatedButton.styleFrom(
                        backgroundColor: const Color(0xFF4F46E5),
                        foregroundColor: Colors.white,
                        padding: const EdgeInsets.symmetric(vertical: 16),
                        shape: RoundedRectangleBorder(
                          borderRadius: BorderRadius.circular(12),
                        ),
                        elevation: 0,
                      ),
                      onPressed: () async {
                        // Confirm from user
                        final confirm = await showDialog<bool>(
                          context: context,
                          builder: (context) => AlertDialog(
                            title: const Text('Move wishlist to cart?'),
                            content: const Text(
                              'This will add all wishlisted items to your shopping cart and remove them from your wishlist.',
                            ),
                            actions: [
                              TextButton(
                                onPressed: () => Navigator.pop(context, false),
                                child: const Text('Cancel'),
                              ),
                              TextButton(
                                onPressed: () => Navigator.pop(context, true),
                                child: const Text('Move All'),
                              ),
                            ],
                          ),
                        );

                        if (confirm != true || !context.mounted) return;

                        // Show progress indicator
                        showDialog(
                          context: context,
                          barrierDismissible: false,
                          builder: (context) => const Center(
                            child: CircularProgressIndicator(),
                          ),
                        );

                        try {
                          final productsToMove = List<Product>.from(
                            wishlistProvider.wishlistedProducts,
                          );
                          for (final product in productsToMove) {
                            await cartProvider.addItem(product);
                            await wishlistProvider.toggleWishlist(product.id);
                          }

                          if (context.mounted) {
                            Navigator.pop(context); // Close loading indicator
                            ScaffoldMessenger.of(context).showSnackBar(
                              SnackBar(
                                content: const Text(
                                  'All items moved to cart successfully!',
                                ),
                                backgroundColor: Colors.green,
                                behavior: SnackBarBehavior.floating,
                                shape: RoundedRectangleBorder(
                                  borderRadius: BorderRadius.circular(8),
                                ),
                              ),
                            );
                          }
                        } catch (e) {
                          if (context.mounted) {
                            Navigator.pop(context); // Close loading indicator
                            ErrorHandler.showError(context, e);
                          }
                        }
                      },
                      child: const Row(
                        mainAxisAlignment: MainAxisAlignment.center,
                        children: [
                          Icon(Icons.shopping_cart_outlined),
                          SizedBox(width: 8),
                          Text(
                            "Move All to Cart",
                            style: TextStyle(
                              fontSize: 16,
                              fontWeight: FontWeight.bold,
                            ),
                          ),
                        ],
                      ),
                    ),
                  ),
                )
              : null,
        );
      },
    );
  }
}
