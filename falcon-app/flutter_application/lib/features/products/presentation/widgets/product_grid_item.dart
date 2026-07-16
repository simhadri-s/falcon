import 'package:flutter/material.dart';
import 'package:flutter_application/core/utils/price_formatter.dart';
import 'package:provider/provider.dart';
import 'package:flutter_application/core/widgets/retry_image.dart';
import 'package:flutter_application/core/utils/auth_guard.dart';
import 'package:flutter_application/core/utils/error_handler.dart';
import 'package:flutter_application/features/home/data/product_model.dart';
import 'package:flutter_application/features/cart/presentation/providers/cart_provider.dart';
import 'package:flutter_application/features/wishlist/presentation/providers/wishlist_provider.dart';
import 'package:flutter_application/features/products/presentation/pages/product_detail_page.dart';

class ProductGridItem extends StatelessWidget {
  final Product product;
  final bool isSelected;
  final Object? heroTag;
  final VoidCallback? onCartTap;

  const ProductGridItem({
    super.key,
    required this.product,
    this.isSelected = false,
    this.heroTag,
    this.onCartTap,
  });

  @override
  Widget build(BuildContext context) {
    final bool hasPrice = product.hasPrice;

    return GestureDetector(
      onTap: () {
        Navigator.push(
          context,
          MaterialPageRoute(
            builder: (context) => ProductDetailPage(
              product: product,
              heroTag: heroTag ?? 'product-${product.id}',
            ),
          ),
        );
      },
      child: Container(
        decoration: BoxDecoration(
          color: Colors.white,
          borderRadius: BorderRadius.circular(12),
          border: isSelected
              ? Border.all(color: const Color(0xFF4F46E5), width: 2)
              : null,
          boxShadow: [
            BoxShadow(
              color: Colors.black.withValues(alpha: 0.06),
              blurRadius: 14,
              offset: const Offset(0, 5),
            ),
          ],
        ),
        clipBehavior: Clip.antiAlias,
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Expanded(
              flex: 5,
              child: Stack(
                fit: StackFit.expand,
                children: [
                  Container(color: Colors.grey.shade50),
                  ClipRRect(
                    borderRadius: const BorderRadius.vertical(
                      top: Radius.circular(12),
                    ),
                    child: Hero(
                      tag: heroTag ?? 'product-${product.id}',
                      child: RetryNetworkImage(
                        imageUrl: product.imageUrls.isNotEmpty
                            ? product.imageUrls.first
                            : 'https://via.placeholder.com/300',
                        width: double.infinity,
                        fit: BoxFit.cover,
                      ),
                    ),
                  ),
                  if (product.hasDiscount)
                    Positioned(
                      top: 8,
                      left: 8,
                      child: Container(
                        padding: const EdgeInsets.symmetric(
                          horizontal: 7,
                          vertical: 3,
                        ),
                        decoration: BoxDecoration(
                          color: const Color(0xFFE53935),
                          borderRadius: BorderRadius.circular(4),
                        ),
                        child: const Text(
                          'SALE',
                          style: TextStyle(
                            color: Colors.white,
                            fontSize: 9,
                            fontWeight: FontWeight.w800,
                            letterSpacing: 0.4,
                          ),
                        ),
                      ),
                    ),
                  Positioned(
                    top: 8,
                    right: 8,
                    child: Consumer<WishlistProvider>(
                      builder: (context, wishlist, child) {
                        final isWishlisted =
                            wishlist.isWishlisted(product.id);
                        return GestureDetector(
                          onTap: () async {
                            final success =
                                await wishlist.toggleWishlist(product.id);
                            if (!success && context.mounted) {
                              ScaffoldMessenger.of(context).showSnackBar(
                                const SnackBar(
                                  content: Text(
                                    'Please log in to add to wishlist',
                                  ),
                                ),
                              );
                            }
                          },
                          child: Container(
                            padding: const EdgeInsets.all(6),
                            decoration: BoxDecoration(
                              color: Colors.white.withValues(alpha: 0.92),
                              shape: BoxShape.circle,
                              boxShadow: [
                                BoxShadow(
                                  color: Colors.black.withValues(alpha: 0.08),
                                  blurRadius: 6,
                                  offset: const Offset(0, 2),
                                ),
                              ],
                            ),
                            child: Icon(
                              isWishlisted
                                  ? Icons.favorite
                                  : Icons.favorite_border,
                              size: 16,
                              color: isWishlisted
                                  ? const Color(0xFFE53935)
                                  : Colors.grey.shade500,
                            ),
                          ),
                        );
                      },
                    ),
                  ),
                ],
              ),
            ),
            Expanded(
              flex: 5,
              child: Padding(
                padding: const EdgeInsets.fromLTRB(10, 10, 10, 10),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Text(
                      product.name,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: const TextStyle(
                        fontWeight: FontWeight.w600,
                        fontSize: 13,
                        color: Color(0xFF1A1A2E),
                      ),
                    ),
                    Container(
                      padding: const EdgeInsets.symmetric(
                        horizontal: 6,
                        vertical: 2,
                      ),
                      decoration: BoxDecoration(
                        color: const Color(0xFF4F46E5).withValues(alpha: 0.08),
                        borderRadius: BorderRadius.circular(4),
                      ),
                      child: Text(
                        product.category.isNotEmpty
                            ? product.category.toUpperCase()
                            : 'PREMIUM',
                        style: const TextStyle(
                          fontSize: 8,
                          fontWeight: FontWeight.w700,
                          color: Color(0xFF4F46E5),
                          letterSpacing: 0.4,
                        ),
                      ),
                    ),
                    Expanded(
                      child: Text(
                        product.description.isNotEmpty
                            ? product.description
                            : 'High quality product.',
                        maxLines: 2,
                        overflow: TextOverflow.ellipsis,
                        style: TextStyle(
                          fontSize: 10,
                          color: Colors.grey.shade500,
                          height: 1.4,
                        ),
                      ),
                    ),
                    Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      crossAxisAlignment: CrossAxisAlignment.end,
                      children: [
                        if (hasPrice)
                          Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              if (product.hasOffer)
                                Container(
                                  margin: const EdgeInsets.only(bottom: 4),
                                  padding: const EdgeInsets.symmetric(horizontal: 4, vertical: 2),
                                  decoration: BoxDecoration(
                                    color: Colors.green.shade50,
                                    borderRadius: BorderRadius.circular(4),
                                  ),
                                  child: Text(
                                    product.offerName,
                                    style: TextStyle(
                                      fontSize: 7,
                                      fontWeight: FontWeight.bold,
                                      color: Colors.green.shade700,
                                    ),
                                  ),
                                ),
                              Row(
                                crossAxisAlignment: CrossAxisAlignment.end,
                                children: [
                                  Text(
                                    formatIndianPrice(product.hasOffer ? product.offerPrice : product.price),
                                    style: const TextStyle(
                                      fontSize: 18,
                                      fontWeight: FontWeight.w800,
                                      color: Color(0xFF1A1A2E),
                                    ),
                                  ),
                                  if (product.hasDiscount)
                                    Padding(
                                      padding: const EdgeInsets.only(
                                        left: 6,
                                        bottom: 1,
                                      ),
                                      child: Text(
                                        formatIndianPrice(product.hasOffer ? product.originalPrice : product.mrp),
                                        style: TextStyle(
                                          fontSize: 9,
                                          fontWeight: FontWeight.w600,
                                          color: Colors.grey.shade500,
                                          decoration:
                                              TextDecoration.lineThrough,
                                        ),
                                      ),
                                    ),
                                ],
                              ),
                            ],
                          )
                        else
                          Text(
                            'Get Quote',
                            style: TextStyle(
                              fontSize: 11,
                              color: Colors.grey.shade500,
                              fontWeight: FontWeight.w500,
                            ),
                          ),
                        Consumer<CartProvider>(
                          builder: (context, cart, child) {
                            return GestureDetector(
                              onTap: onCartTap ?? () async {
                                final loggedIn =
                                    await AuthGuard.checkLoginOrRedirect(
                                  context,
                                  message:
                                      'Please login to add items to cart',
                                );
                                if (!context.mounted) return;
                                if (!loggedIn) return;
                                try {
                                  await context
                                      .read<CartProvider>()
                                      .addItem(product);
                                  if (!context.mounted) return;
                                  ScaffoldMessenger.of(context).showSnackBar(
                                    SnackBar(
                                      content: Text(
                                        '${product.name} added to cart',
                                      ),
                                      duration: const Duration(seconds: 2),
                                      behavior: SnackBarBehavior.floating,
                                      backgroundColor:
                                          const Color(0xFF4F46E5),
                                      shape: RoundedRectangleBorder(
                                        borderRadius:
                                            BorderRadius.circular(8),
                                      ),
                                    ),
                                  );
                                } catch (e) {
                                  if (!context.mounted) return;
                                  ErrorHandler.showError(context, e);
                                }
                              },
                              child: Container(
                                padding: const EdgeInsets.all(7),
                                decoration: BoxDecoration(
                                  gradient: const LinearGradient(
                                    colors: [
                                      Color(0xFF4F46E5),
                                      Color(0xFF7C3AED),
                                    ],
                                  ),
                                  borderRadius: BorderRadius.circular(8),
                                ),
                                child: const Icon(
                                  Icons.add_shopping_cart,
                                  color: Colors.white,
                                  size: 16,
                                ),
                              ),
                            );
                          },
                        ),
                      ],
                    ),
                  ],
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

