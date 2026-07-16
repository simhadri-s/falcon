import 'package:flutter/foundation.dart';
import 'package:flutter_application/features/wishlist/data/wishlist_api_service.dart';
import 'package:flutter_application/features/home/data/product_model.dart';
import 'package:flutter_application/features/auth/data/auth_service.dart';

class WishlistProvider extends ChangeNotifier {
  final WishlistApiService _apiService = WishlistApiService();
  final AuthService _authService = AuthService();
  
  Set<String> _wishlistedProductIds = {};
  List<Product> _wishlistedProducts = [];
  bool _isLoading = false;

  Set<String> get wishlistedProductIds => _wishlistedProductIds;
  List<Product> get wishlistedProducts => _wishlistedProducts;
  bool get isLoading => _isLoading;

  bool isWishlisted(String productId) {
    return _wishlistedProductIds.contains(productId);
  }

  Future<void> fetchWishlist() async {
    final isLoggedIn = await _authService.isLoggedIn();
    if (!isLoggedIn) {
      _wishlistedProductIds = {};
      _wishlistedProducts = [];
      notifyListeners();
      return;
    }

    _isLoading = true;
    notifyListeners();

    try {
      final response = await _apiService.getWishlist();
      _wishlistedProducts = response.products;
      _wishlistedProductIds = response.products.map((p) => p.id).toSet();
    } catch (e) {
      debugPrint("Error fetching wishlist: $e");
      // Don't clear on error, might just be network issue
    } finally {
      _isLoading = false;
      notifyListeners();
    }
  }

  Future<bool> toggleWishlist(String productId) async {
    final isLoggedIn = await _authService.isLoggedIn();
    if (!isLoggedIn) {
      return false; // Indicating user needs to log in
    }

    // Optimistic update
    final wasWishlisted = _wishlistedProductIds.contains(productId);
    if (wasWishlisted) {
      _wishlistedProductIds.remove(productId);
      _wishlistedProducts.removeWhere((p) => p.id == productId);
    } else {
      _wishlistedProductIds.add(productId);
      // Note: we don't have the full product details to add to _wishlistedProducts 
      // optimally here without fetching. The API will return the updated list.
    }
    notifyListeners();

    try {
      final response = await _apiService.toggleWishlist(productId);
      _wishlistedProducts = response.products;
      _wishlistedProductIds = response.products.map((p) => p.id).toSet();
      notifyListeners();
      return true;
    } catch (e) {
      debugPrint("Error toggling wishlist: $e");
      // Revert optimistic update on error
      if (wasWishlisted) {
        _wishlistedProductIds.add(productId);
      } else {
        _wishlistedProductIds.remove(productId);
      }
      notifyListeners();
      throw e;
    }
  }
  
  void clear() {
    _wishlistedProductIds = {};
    _wishlistedProducts = [];
    notifyListeners();
  }
}
