import 'package:flutter/material.dart';
import 'package:flutter_application/core/utils/price_formatter.dart';
import 'package:provider/provider.dart';
import 'package:flutter_application/features/cart/presentation/providers/cart_provider.dart';
import 'package:flutter_application/features/cart/presentation/pages/checkout_page.dart';
import 'package:flutter_application/core/widgets/retry_image.dart';
import 'package:flutter_application/features/products/presentation/pages/product_detail_page.dart';
import 'package:flutter_application/core/navigation/main_nav.dart';
import 'package:flutter_application/core/utils/error_handler.dart';

class CartPage extends StatefulWidget {
  const CartPage({super.key});

  @override
  State<CartPage> createState() => _CartPageState();
}

class _CartPageState extends State<CartPage> {
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      context.read<CartProvider>().fetchCart();
    });
  }

  String _formatPrice(double value) => formatIndianPrice(value);

  @override
  Widget build(BuildContext context) {
    final cart = context.watch<CartProvider>();

    return Scaffold(
      backgroundColor: Colors.grey.shade50,
      appBar: AppBar(
        title: const Text(
          'My Cart',
          style: TextStyle(color: Colors.black, fontWeight: FontWeight.bold),
        ),
        backgroundColor: Colors.white,
        elevation: 0,
        centerTitle: true,
        actions: [
          if (cart.items.isNotEmpty)
            IconButton(
              icon: const Icon(Icons.delete_outline, color: Colors.redAccent),
              onPressed: () => _showClearCartDialog(context, cart),
            ),
        ],
      ),
      body: cart.isLoading && cart.items.isEmpty
          ? const Center(
              child: CircularProgressIndicator(color: Color(0xFF0284C7)),
            )
          : cart.items.isEmpty
          ? RefreshIndicator(
              color: const Color(0xFF0284C7),
              onRefresh: () => context.read<CartProvider>().fetchCart(),
              child: SingleChildScrollView(
                physics: const AlwaysScrollableScrollPhysics(),
                child: SizedBox(
                  height: MediaQuery.of(context).size.height * 0.7,
                  child: _buildEmptyCart(context),
                ),
              ),
            )
          : Column(
              children: [
                Expanded(
                  child: RefreshIndicator(
                    color: const Color(0xFF0284C7),
                    onRefresh: () => context.read<CartProvider>().fetchCart(),
                    child: ListView(
                      physics: const AlwaysScrollableScrollPhysics(),
                      padding: const EdgeInsets.symmetric(
                        horizontal: 16,
                        vertical: 20,
                      ),
                      children: [
                        if (cart.activeItems.isNotEmpty) ...[
                          Row(
                            children: [
                              Expanded(
                                child: Text(
                                  "Items in Cart (${cart.activeItems.length})",
                                  style: const TextStyle(
                                    fontWeight: FontWeight.bold,
                                    fontSize: 18,
                                  ),
                                  overflow: TextOverflow.ellipsis,
                                ),
                              ),
                              TextButton.icon(
                                onPressed: () => _toggleAllItems(cart, false),
                                icon: const Icon(Icons.archive_outlined, size: 18),
                                label: const Text("Save all for later"),
                                style: TextButton.styleFrom(
                                  foregroundColor: Colors.deepPurple,
                                  padding: EdgeInsets.zero,
                                ),
                              ),
                            ],
                          ),
                          const SizedBox(height: 12),
                          ...cart.activeItems.map((item) => _buildCartItem(context, item, cart, true)),
                        ],
                        if (cart.savedItems.isNotEmpty) ...[
                          const SizedBox(height: 30),
                          Row(
                            children: [
                              Expanded(
                                child: Text(
                                  "Saved for Later (${cart.savedItems.length})",
                                  style: const TextStyle(
                                    fontWeight: FontWeight.bold,
                                    fontSize: 18,
                                  ),
                                  overflow: TextOverflow.ellipsis,
                                ),
                              ),
                              TextButton.icon(
                                onPressed: () => _toggleAllItems(cart, true),
                                icon: const Icon(Icons.unarchive_outlined, size: 18),
                                label: const Text("Move all to cart"),
                                style: TextButton.styleFrom(
                                  foregroundColor: Colors.deepPurple,
                                  padding: EdgeInsets.zero,
                                ),
                              ),
                            ],
                          ),
                          const SizedBox(height: 12),
                          ...cart.savedItems.map((item) => _buildCartItem(context, item, cart, false)),
                        ],
                      ],
                    ),
                  ),
                ),
        ),
      ),
    );
  }

  Widget _buildQtyBtn(IconData icon, VoidCallback onTap) {
    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(20),
      child: Padding(
        padding: const EdgeInsets.all(4.0),
        child: Icon(icon, size: 16, color: Colors.black87),
      ),
    );
  }

  Widget _buildOrderSummary(BuildContext context, CartProvider cart) {
    return Container(
      padding: const EdgeInsets.all(24),
      decoration: const BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.vertical(top: Radius.circular(30)),
        boxShadow: [
          BoxShadow(
            color: Colors.black12,
            blurRadius: 20,
            offset: Offset(0, -5),
          ),
        ],
      ),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          _buildSummaryRow('Subtotal', _formatPrice(cart.subtotal)),
          if (cart.totalDiscount > 0) ...[
            const SizedBox(height: 8),
            _buildSummaryRow(
              'Offer Discount',
              '-${_formatPrice(cart.totalDiscount)}',
              valueColor: Colors.green,
            ),
          ],
          const SizedBox(height: 12),
          const Divider(),
          const SizedBox(height: 12),
          _buildSummaryRow(
            'Total Amount',
            _formatPrice(cart.totalPrice),
            emphasize: true,
          ),
          const SizedBox(height: 20),
          SizedBox(
            width: double.infinity,
            child: ElevatedButton(
              onPressed: () {
                Navigator.push(
                  context,
                  MaterialPageRoute(builder: (context) => const CheckoutPage()),
                );
              },
              style: ElevatedButton.styleFrom(
                backgroundColor: const Color(0xFF0284C7),
                padding: const EdgeInsets.symmetric(vertical: 16),
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(16),
                ),
                elevation: 0,
              ),
              child: const Text(
                'Checkout',
                style: TextStyle(
                  fontSize: 16,
                  fontWeight: FontWeight.bold,
                  color: Colors.white,
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildSummaryRow(
    String label,
    String value, {
    bool emphasize = false,
    Color? valueColor,
  }) {
    return Row(
      mainAxisAlignment: MainAxisAlignment.spaceBetween,
      children: [
        Text(
          label,
          style: TextStyle(
            fontSize: emphasize ? 18 : 14,
            fontWeight: emphasize ? FontWeight.w800 : FontWeight.w500,
            color: emphasize ? const Color(0xFF111827) : Colors.grey.shade600,
          ),
        ),
        Text(
          value,
          style: TextStyle(
            fontSize: emphasize ? 22 : 15,
            fontWeight: emphasize ? FontWeight.w900 : FontWeight.w700,
            color: valueColor ?? (emphasize ? const Color(0xFF0284C7) : const Color(0xFF111827)),
          ),
        ),
      ],
    );
  }

  void _showClearCartDialog(BuildContext context, CartProvider cart) {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Clear Cart'),
        content: const Text(
          'Are you sure you want to remove all items from your cart?',
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('Cancel'),
          ),
          TextButton(
            onPressed: () async {
              Navigator.pop(context);
              try {
                await cart.clear();
              } catch (e) {
                if (context.mounted) {
                  ErrorHandler.showError(context, e);
                }
              }
            },
            child: const Text('Clear', style: TextStyle(color: Colors.red)),
          ),
        ],
      ),
    );
  }
}
