import 'package:flutter/material.dart';
import 'package:flutter_application/features/cart/data/cart_item_model.dart';
import 'package:flutter_application/features/cart/data/cart_model.dart';
import 'package:flutter_application/features/cart/data/cart_service.dart';
import 'package:flutter_application/features/home/data/product_model.dart';

class CartProvider with ChangeNotifier {
  final CartService _cartService = CartService();
  List<CartItem> _items = [];
  Cart? _cart;
  bool _isLoading = false;

  CartProvider() {
    fetchCart();
  }

  List<CartItem> get items => [..._items];
  List<CartItem> get activeItems => _items.where((i) => i.active).toList();
  List<CartItem> get savedItems => _items.where((i) => !i.active).toList();
  Cart? get cart => _cart;
  bool get isLoading => _isLoading;
  int get itemCount => activeItems.length;
  int get totalQuantity =>
      activeItems.fold(0, (sum, item) => sum + item.quantity);

  double get subtotal =>
      _cart?.subtotal ??
      _items.fold(
        0.0,
        (sum, item) => sum + (item.product.price * item.quantity),
      );
  double get totalDiscount => _cart?.totalDiscount ?? 0.0;
  double get totalPrice =>
      _cart?.totalAmount ??
      activeItems.fold(0.0, (sum, item) => sum + item.total);

  Future<void> fetchCart() async {
    await _syncCart(showLoader: true);
  }

  Future<void> addItem(Product product, {int quantity = 1, String? variantId}) async {
    final index = _items.indexWhere((item) => item.product.id == product.id && item.variantId == variantId);
    final currentQty = index >= 0 ? _items[index].quantity : 0;

    if (product.manageStock) {
      if (currentQty + quantity > product.stockQuantity) {
        throw Exception(
          "Only ${product.stockQuantity} units of this product are available.",
        );
      }
    }

    // Optimistic UI update
    if (index >= 0) {
      _items[index].quantity += quantity;
    } else {
      _items.add(CartItem(product: product, quantity: quantity, variantId: variantId));
    }
    notifyListeners();

    // Sync with backend
    try {
      await _cartService.addToCart(product.id, quantity, variantId: variantId);
      await _syncCart(showLoader: false);
    } catch (e) {
      await _syncCart(showLoader: false);
      rethrow;
    }
  }

  Future<void> updateCartQuantity(String productId, int quantity, {String? variantId}) async {
    final index = _items.indexWhere((item) => item.product.id == productId && item.variantId == variantId);
    if (index >= 0) {
      final product = _items[index].product;
      if (product.manageStock && quantity > product.stockQuantity) {
        throw Exception(
          "Only ${product.stockQuantity} units of this product are available.",
        );
      }

      if (quantity > 0) {
        _items[index].quantity = quantity;
        notifyListeners();

        try {
          await _cartService.updateQuantity(productId, quantity, variantId: variantId);
        } catch (e) {
          await fetchCart();
          rethrow;
        }
      } else {
        await removeItem(productId, variantId: variantId);
      }
    }
  }

  Future<void> incrementQuantity(String productId, {String? variantId}) async {
    final index = _items.indexWhere((item) => item.product.id == productId && item.variantId == variantId);
    if (index >= 0) {
      final product = _items[index].product;
      final newQuantity = _items[index].quantity + 1;

      if (product.manageStock && newQuantity > product.stockQuantity) {
        throw Exception(
          "Only ${product.stockQuantity} units of this product are available.",
        );
      }

      _items[index].quantity = newQuantity;
      notifyListeners();

      try {
        await _cartService.updateQuantity(productId, newQuantity, variantId: variantId);
      } catch (e) {
        await fetchCart();
        rethrow;
      }
    }
  }

  Future<void> decrementQuantity(String productId, {String? variantId}) async {
    final index = _items.indexWhere((item) => item.product.id == productId && item.variantId == variantId);
    if (index >= 0) {
      if (_items[index].quantity > 1) {
        final newQuantity = _items[index].quantity - 1;
        _items[index].quantity = newQuantity;
        notifyListeners();

        try {
          await _cartService.updateQuantity(productId, newQuantity, variantId: variantId);
        } catch (e) {
          await fetchCart();
          rethrow;
        }
      } else {
        await removeItem(productId, variantId: variantId);
      }
    }
  }

  Future<void> removeItem(String productId, {String? variantId}) async {
    final removedItem = _items.firstWhere(
      (item) => item.product.id == productId && item.variantId == variantId,
    );
    _items.removeWhere((item) => item.product.id == productId && item.variantId == variantId);
    notifyListeners();

    try {
      await _cartService.deleteItem(productId, variantId: variantId);
    } catch (e) {
      _items.add(removedItem);
      notifyListeners();
      rethrow;
    }
  }

  Future<void> clear() async {
    final oldItems = [..._items];
    _items.clear();
    notifyListeners();

    try {
      await _cartService.clearCart();
    } catch (e) {
      _items = oldItems;
      notifyListeners();
      rethrow;
    }
  }

  Future<void> toggleActiveStatus(String productId, bool active, {String? variantId}) async {
    final index = _items.indexWhere((item) => item.product.id == productId && item.variantId == variantId);
    if (index >= 0) {
      final oldItem = _items[index];
      _items[index] = CartItem(
        product: oldItem.product,
        quantity: oldItem.quantity,
        active: active,
        variantId: oldItem.variantId,
        variantAttributes: oldItem.variantAttributes,
        variantPrice: oldItem.variantPrice,
      );
      notifyListeners();

      try {
        await _cartService.updateActiveStatus(productId, active, variantId: variantId);
      } catch (e) {
        _items[index] = oldItem;
        notifyListeners();
        rethrow;
      }
    }
  }

  Future<void> toggleAllActiveStatus(bool active) async {
    final oldItems = [..._items];
    _items = _items
        .map(
          (item) => CartItem(
            product: item.product,
            quantity: item.quantity,
            active: active,
            variantId: item.variantId,
            variantAttributes: item.variantAttributes,
            variantPrice: item.variantPrice,
          ),
        )
        .toList();
    notifyListeners();

    try {
      await _cartService.updateAllActiveStatus(active);
    } catch (e) {
      _items = oldItems;
      notifyListeners();
      rethrow;
    }
  }

  Future<void> _syncCart({required bool showLoader}) async {
    if (showLoader) {
      _isLoading = true;
      notifyListeners();
    }

    final latestCart = await _cartService.tryGetCart();
    if (latestCart != null) {
      _cart = latestCart;
      _items = latestCart.items;
    }

    if (showLoader) {
      _isLoading = false;
    }

    notifyListeners();
  }
}
