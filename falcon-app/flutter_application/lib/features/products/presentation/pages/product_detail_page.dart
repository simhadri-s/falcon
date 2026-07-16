import 'package:flutter/material.dart';
import 'package:flutter_application/core/utils/price_formatter.dart';
import 'package:flutter_application/core/widgets/retry_image.dart';
import 'package:flutter_application/features/home/data/product_model.dart';
import 'package:provider/provider.dart';
import 'package:share_plus/share_plus.dart';
import 'package:flutter_application/features/cart/presentation/providers/cart_provider.dart';
import 'package:flutter_application/core/utils/error_handler.dart';
import 'package:flutter_application/core/utils/auth_guard.dart';
import 'package:flutter_application/features/cart/data/cart_item_model.dart';
import 'package:flutter_application/features/cart/presentation/pages/checkout_page.dart';
import 'package:flutter_application/features/cart/presentation/pages/cart_page.dart';
import 'package:flutter_application/features/wishlist/presentation/providers/wishlist_provider.dart';
import 'package:flutter_application/features/products/data/products_api_service.dart';
import 'package:flutter_application/features/home/presentation/widgets/product_card.dart';
import 'package:flutter_application/features/reviews/presentation/widgets/review_summary_widget.dart';
import 'package:flutter_application/features/reviews/presentation/widgets/review_submission_form.dart';
import 'package:flutter_application/features/reviews/presentation/widgets/review_list_widget.dart';

class ProductDetailPage extends StatefulWidget {
  final Product product;
  final Object? heroTag;

  const ProductDetailPage({super.key, required this.product, this.heroTag});

  @override
  State<ProductDetailPage> createState() => _ProductDetailPageState();
}

class _ProductDetailPageState extends State<ProductDetailPage> {
  int _currentImageIndex = 0;
  bool _isExpanded = false;
  int _buyNowQuantity = 1;
  int? _lastSeenCartQuantity;
  Key _reviewListKey = UniqueKey();

  late Product _currentProduct;
  ProductVariant? _selectedVariant;

  final ProductsApiService _productsApiService = ProductsApiService();
  late Future<List<Product>> _similarProductsFuture;

  void _onReviewSubmitted() {
    setState(() {
      _reviewListKey = UniqueKey();
    });
    _refreshProductDetails();
  }

  Future<void> _refreshProductDetails() async {
    try {
      final updatedProduct = await _productsApiService.getProductById(_currentProduct.id);
      if (updatedProduct != null && mounted) {
        setState(() {
          _currentProduct = updatedProduct;
          if (_currentProduct.hasVariants && _currentProduct.variants.isNotEmpty) {
            if (_selectedVariant != null) {
              final found = _currentProduct.variants.where((v) => v.id == _selectedVariant!.id).toList();
              if (found.isNotEmpty) _selectedVariant = found.first;
              else _selectedVariant = _currentProduct.variants.first;
            } else {
              _selectedVariant = _currentProduct.variants.first;
            }
          }
        });
      }
    } catch (e) {
      debugPrint('Error refreshing product details: $e');
    }
  }

  @override
  void initState() {
    super.initState();
    _currentProduct = widget.product;
    if (_currentProduct.hasVariants && _currentProduct.variants.isNotEmpty) {
      _selectedVariant = _currentProduct.variants.first;
    }
    _similarProductsFuture = _fetchSimilarProducts();
  }

  @override
  void didUpdateWidget(covariant ProductDetailPage oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.product.id != widget.product.id) {
      setState(() {
        _currentProduct = widget.product;
        if (_currentProduct.hasVariants && _currentProduct.variants.isNotEmpty) {
          _selectedVariant = _currentProduct.variants.first;
        } else {
          _selectedVariant = null;
        }
        _currentImageIndex = 0;
        _reviewListKey = UniqueKey();
        _similarProductsFuture = _fetchSimilarProducts();
      });
    }
  }

  Future<List<Product>> _fetchSimilarProducts() async {
    try {
      final List<Product> list;
      if (_currentProduct.industries.isNotEmpty) {
        final slug = _currentProduct.industries.first.slug;
        final paginated = await _productsApiService.getProductsByIndustry(
          slug,
          size: 10,
        );
        list = paginated.products;
      } else {
        final paginated = await _productsApiService.getAllProducts(
          category: _currentProduct.category,
          size: 10,
        );
        list = paginated.products;
      }
      // Filter out the currently viewed product to avoid duplicates
      return list.where((p) => p.id != _currentProduct.id).toList();
    } catch (e) {
      debugPrint('Error fetching similar products: $e');
      return [];
    }
  }

  String _formatPrice(double value) => formatIndianPrice(value);

  @override
  Widget build(BuildContext context) {
    final cart = context.watch<CartProvider>();
    final cartIndex = cart.items.indexWhere(
      (item) => item.product.id == _currentProduct.id && item.variantId == _selectedVariant?.id,
    );
    final int quantityInCart = cartIndex >= 0
        ? cart.items[cartIndex].quantity
        : 0;

    if (_lastSeenCartQuantity != quantityInCart) {
      _lastSeenCartQuantity = quantityInCart;
      if (quantityInCart > 0) {
        _buyNowQuantity = quantityInCart;
      } else {
        _buyNowQuantity = 1;
      }
    }
    
    final double price = _currentProduct.hasVariants && _selectedVariant != null
        ? (_selectedVariant!.sellingPrice > 0 ? _selectedVariant!.sellingPrice : _selectedVariant!.mrp)
        : (_currentProduct.sellingPrice > 0 ? _currentProduct.sellingPrice : _currentProduct.mrp);

    final double mrp = _currentProduct.hasVariants && _selectedVariant != null
        ? _selectedVariant!.mrp
        : _currentProduct.mrp;

    final int stockQuantity = _currentProduct.hasVariants && _selectedVariant != null
        ? _selectedVariant!.stockQuantity
        : _currentProduct.stockQuantity;

    final bool isOutOfStock = _currentProduct.manageStock && stockQuantity <= 0;
    
    final int discountPercentage = (mrp > 0 && mrp > price ? (((mrp - price) / mrp) * 100).round() : 0);

    return Scaffold(
      backgroundColor: Colors.white,
      appBar: AppBar(
        backgroundColor: Colors.white,
        elevation: 0,
        leading: IconButton(
          icon: const Icon(
            Icons.arrow_back_ios_new,
            color: Colors.black,
            size: 20,
          ),
          onPressed: () => Navigator.pop(context),
        ),
        actions: [
          Consumer<WishlistProvider>(
            builder: (context, wishlist, child) {
              final isWishlisted = wishlist.isWishlisted(widget.product.id);
              return IconButton(
                icon: Icon(
                  isWishlisted ? Icons.favorite : Icons.favorite_border,
                  color: isWishlisted ? Colors.red : Colors.black,
                ),
                onPressed: () async {
                  final success = await wishlist.toggleWishlist(widget.product.id);
                  if (!success && context.mounted) {
                    ScaffoldMessenger.of(context).showSnackBar(
                      const SnackBar(content: Text('Please log in to add to wishlist')),
                    );
                  }
                },
              );
            },
          ),
          IconButton(
            icon: const Icon(Icons.share_outlined, color: Colors.black),
            onPressed: () {
              Share.share(
                'Check out this ${_currentProduct.name} on Falcon App!\n\n${_currentProduct.description}',
              );
            },
          ),
          IconButton(
            icon: Badge(
              label: Text("${cart.totalQuantity}"),
              isLabelVisible: cart.totalQuantity > 0,
              child: const Icon(
                Icons.shopping_cart_outlined,
                color: Colors.black,
              ),
            ),
            onPressed: () {
              Navigator.push(
                context,
                MaterialPageRoute(builder: (context) => const CartPage()),
              );
            },
          ),
          const SizedBox(width: 8),
        ],
      ),
      body: SingleChildScrollView(
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // Image Carousel
            _buildImageCarousel(),

            Padding(
              padding: const EdgeInsets.all(20.0),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  // Product Title & Brand
                  Text(
                    _currentProduct.name,
                    style: const TextStyle(
                      fontSize: 24,
                      fontWeight: FontWeight.w900,
                      letterSpacing: -0.5,
                    ),
                  ),
                  const SizedBox(height: 4),
                  Text(
                    "${_currentProduct.category} Premium Collection",
                    style: TextStyle(
                      fontSize: 14,
                      color: Colors.grey.shade600,
                      fontWeight: FontWeight.w500,
                    ),
                  ),
                  if (_currentProduct.hasPrice) ...[
                    const SizedBox(height: 16),
                    Row(
                      crossAxisAlignment: CrossAxisAlignment.end,
                      children: [
                        Text(
                          _formatPrice(price),
                          style: const TextStyle(
                            fontSize: 30,
                            fontWeight: FontWeight.w900,
                            letterSpacing: -0.8,
                            color: Color(0xFF111827),
                          ),
                        ),
                        if (discountPercentage > 0)
                          Padding(
                            padding: const EdgeInsets.only(left: 12),
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              mainAxisSize: MainAxisSize.min,
                              children: [
                                Container(
                                  padding: const EdgeInsets.symmetric(
                                    horizontal: 6,
                                    vertical: 3,
                                  ),
                                  decoration: BoxDecoration(
                                    color: const Color(0xFFE53935),
                                    borderRadius: BorderRadius.circular(6),
                                  ),
                                  child: Text(
                                    '$discountPercentage% OFF',
                                    style: const TextStyle(
                                      color: Colors.white,
                                      fontSize: 10,
                                      fontWeight: FontWeight.w800,
                                      letterSpacing: 0.3,
                                    ),
                                  ),
                                ),
                                const SizedBox(height: 4),
                                Text(
                                  _formatPrice(mrp),
                                  style: TextStyle(
                                    fontSize: 14,
                                    color: Colors.grey.shade500,
                                    fontWeight: FontWeight.w600,
                                    decoration: TextDecoration.lineThrough,
                                  ),
                                ),
                              ],
                            ),
                          ),
                      ],
                    ),
                  ],

                  if (_currentProduct.manageStock) ...[
                    const SizedBox(height: 12),
                    Container(
                      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
                      decoration: BoxDecoration(
                        color: stockQuantity > 0 ? const Color(0xFFE8F5E9) : const Color(0xFFFFEBEE),
                        borderRadius: BorderRadius.circular(6),
                        border: Border.all(
                          color: stockQuantity > 0 ? const Color(0xFFC8E6C9) : const Color(0xFFFFCDD2),
                        ),
                      ),
                      child: Row(
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          Icon(
                            stockQuantity > 0 ? Icons.check_circle_outline : Icons.error_outline,
                            size: 16,
                            color: stockQuantity > 0 ? const Color(0xFF2E7D32) : const Color(0xFFC62828),
                          ),
                          const SizedBox(width: 6),
                          Text(
                            stockQuantity > 10
                              ? "In Stock" :
                              stockQuantity > 0
                                  ? "Only $stockQuantity units available in stock!"
                                  : "Out of Stock",
                            style: TextStyle(
                              color: stockQuantity > 0 ? const Color(0xFF2E7D32) : const Color(0xFFC62828),
                              fontSize: 12,
                              fontWeight: FontWeight.w700,
                            ),
                          ),
                        ],
                      ),
                    ),
                  ],

                  const SizedBox(height: 24),
                  
                  if (_currentProduct.hasVariants && _currentProduct.variants.isNotEmpty) ...[
                    const Text(
                      "Select Variant",
                      style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
                    ),
                    const SizedBox(height: 12),
                    Wrap(
                      spacing: 12,
                      runSpacing: 12,
                      children: _currentProduct.variants.map((variant) {
                        final isSelected = _selectedVariant?.id == variant.id;
                        final title = variant.attributes.values.join(' / ');
                        return ChoiceChip(
                          label: Text(
                            title.isNotEmpty ? title : variant.sku,
                            style: TextStyle(
                              fontWeight: isSelected ? FontWeight.bold : FontWeight.w500,
                              color: isSelected ? Colors.white : Colors.black87,
                            ),
                          ),
                          selected: isSelected,
                          onSelected: (selected) {
                            if (selected) {
                              setState(() => _selectedVariant = variant);
                            }
                          },
                          selectedColor: Colors.black,
                          backgroundColor: Colors.white,
                          shape: RoundedRectangleBorder(
                            borderRadius: BorderRadius.circular(8),
                            side: BorderSide(
                              color: isSelected ? Colors.black : Colors.grey.shade300,
                              width: 1.5,
                            ),
                          ),
                          padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
                        );
                      }).toList(),
                    ),
                    const SizedBox(height: 24),
                  ],

                  // Product Details Section
                  const Text(
                    "Product Details",
                    style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
                  ),
                  const SizedBox(height: 8),
                  Text(
                    _currentProduct.description,
                    maxLines: _isExpanded ? 100 : 4,
                    overflow: TextOverflow.ellipsis,
                    style: TextStyle(
                      fontSize: 15,
                      color: Colors.grey.shade800,
                      height: 1.5,
                    ),
                  ),
                  GestureDetector(
                    onTap: () => setState(() => _isExpanded = !_isExpanded),
                    child: Text(
                      _isExpanded ? "Show Less" : "...More",
                      style: const TextStyle(
                        color: Colors.redAccent,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                  ),

                  const SizedBox(height: 24),

                  // Small Badges (Nearest Store, VIP, Return Policy)
                  SingleChildScrollView(
                    scrollDirection: Axis.horizontal,
                    child: Row(
                      children: [
                        _buildSmallBadge(
                          Icons.location_on_outlined,
                          "Nearest Store",
                        ),
                        _buildSmallBadge(Icons.lock_outline, "VIP"),
                        _buildSmallBadge(Icons.history, "Return policy"),
                      ],
                    ),
                  ),

                  const SizedBox(height: 24),

                  // Buy Now Quantity Selector
                  if (widget.product.hasPrice && !isOutOfStock) ...[
                    _buildBuyNowQuantitySelector(quantityInCart),
                    const SizedBox(height: 16),
                  ],

                   // Action Buttons (Add to Cart, Buy Now)
                  if (isOutOfStock)
                    Container(
                      width: double.infinity,
                      height: 50,
                      decoration: BoxDecoration(
                        color: Colors.grey.shade100,
                        borderRadius: BorderRadius.circular(8),
                        border: Border.all(color: Colors.grey.shade300),
                      ),
                      alignment: Alignment.center,
                      child: Row(
                        mainAxisAlignment: MainAxisAlignment.center,
                        children: [
                          Icon(Icons.remove_shopping_cart, color: Colors.grey.shade500),
                          const SizedBox(width: 8),
                          Text(
                            "OUT OF STOCK",
                            style: TextStyle(
                              color: Colors.grey.shade600,
                              fontWeight: FontWeight.w900,
                              fontSize: 16,
                              letterSpacing: 0.5,
                            ),
                          ),
                        ],
                      ),
                    )
                  else
                    Row(
                      children: [
                        Expanded(
                          child: quantityInCart > 0
                              ? _buildActionButton(
                                  icon: Icons.shopping_cart_outlined,
                                  label: "Go to Cart",
                                  color: const Color(0xFF0284C7),
                                  isOutlined: true,
                                  onTap: () {
                                    Navigator.push(
                                      context,
                                      MaterialPageRoute(
                                        builder: (context) => const CartPage(),
                                      ),
                                    );
                                  },
                                )
                              : _buildActionButton(
                                  icon: Icons.add_shopping_cart,
                                  label: "Add to Cart",
                                  color: const Color(0xFF0284C7),
                                  isOutlined: true,
                                  onTap: () async {
                                    final loggedIn =
                                        await AuthGuard.checkLoginOrRedirect(
                                          context,
                                          message:
                                              'Please login to add items to cart',
                                        );
                                    if (!loggedIn) return;

                                    if (context.mounted) {
                                      try {
                                        await context
                                            .read<CartProvider>()
                                            .addItem(
                                              widget.product,
                                              quantity: _buyNowQuantity,
                                              variantId: _selectedVariant?.id,
                                            );
                                        if (!context.mounted) return;
                                        ScaffoldMessenger.of(
                                          context,
                                        ).showSnackBar(
                                          SnackBar(
                                            content: Text(
                                              '${widget.product.name} added to cart',
                                            ),
                                            behavior: SnackBarBehavior.floating,
                                            backgroundColor: const Color(0xFF0284C7),
                                          ),
                                        );
                                      } catch (e) {
                                        if (!context.mounted) return;
                                        ErrorHandler.showError(context, e);
                                      }
                                    }
                                  },
                                ),
                        ),
                        const SizedBox(width: 12),
                        Expanded(
                          child: _buildActionButton(
                            icon: Icons.touch_app,
                            label: "Buy Now",
                            color: const Color(0xFF0284C7),
                            isOutlined: false,
                            onTap: () async {
                              final loggedIn =
                                  await AuthGuard.checkLoginOrRedirect(
                                    context,
                                    message:
                                        'Please login to proceed with purchase',
                                  );
                              if (!loggedIn) return;
                              if (context.mounted) {
                                Navigator.push(
                                  context,
                                  MaterialPageRoute(
                                    builder: (context) => CheckoutPage(
                                      buyNowItem: CartItem(
                                        product: widget.product,
                                        quantity: _buyNowQuantity,
                                        active: true,
                                        variantId: _selectedVariant?.id,
                                        variantAttributes: _selectedVariant?.attributes,
                                        variantPrice: _selectedVariant != null ? (_selectedVariant!.sellingPrice > 0 ? _selectedVariant!.sellingPrice : _selectedVariant!.mrp) : null,
                                      ),
                                    ),
                                  ),
                                );
                              }
                            },
                          ),
                        ),
                      ],
                    ),

                  const SizedBox(height: 24),

                  if (widget.product.specifications.isNotEmpty)
                    Container(
                      width: double.infinity,
                      padding: const EdgeInsets.all(20),
                      decoration: BoxDecoration(
                        color: const Color(0xFFFFD1D8),
                        borderRadius: BorderRadius.circular(12),
                      ),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          const Text(
                            "Specifications",
                            style: TextStyle(
                              fontSize: 16,
                              fontWeight: FontWeight.bold,
                              color: Colors.black,
                            ),
                          ),
                          const SizedBox(height: 12),
                          Table(
                            columnWidths: const {
                              0: FlexColumnWidth(1),
                              1: FlexColumnWidth(1),
                            },
                            children: widget.product.specifications.entries
                                .map((e) => _buildSpecRow(e.key, e.value))
                                .toList(),
                          ),
                        ],
                      ),
                    ),

                  const SizedBox(height: 16),
                  _buildSimilarProductsSection(),

                  const SizedBox(height: 32),
                  ReviewSummaryWidget(product: _currentProduct),
                  const SizedBox(height: 24),
                  ReviewSubmissionForm(
                    productId: _currentProduct.id,
                    onReviewSubmitted: _onReviewSubmitted,
                  ),
                  const SizedBox(height: 24),
                  ReviewListWidget(
                    productId: _currentProduct.id,
                    listKey: _reviewListKey,
                  ),

                  const SizedBox(height: 40),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildSimilarProductsSection() {
    return FutureBuilder<List<Product>>(
      future: _similarProductsFuture,
      builder: (context, snapshot) {
        if (snapshot.connectionState == ConnectionState.waiting) {
          return const Padding(
            padding: EdgeInsets.only(top: 24.0),
            child: SizedBox(
              height: 260,
              child: Center(
                child: CircularProgressIndicator(
                  color: Color(0xFF0284C7),
                ),
              ),
            ),
          );
        } else if (snapshot.hasError) {
          return const SizedBox.shrink();
        } else if (!snapshot.hasData || snapshot.data!.isEmpty) {
          return const SizedBox.shrink();
        }

        final similarProducts = snapshot.data!;

        return Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const SizedBox(height: 32),
            const Text(
              "Similar Products",
              style: TextStyle(
                fontSize: 18,
                fontWeight: FontWeight.bold,
                letterSpacing: -0.5,
              ),
            ),
            const SizedBox(height: 16),
            SizedBox(
              height: 265,
              child: ListView.builder(
                scrollDirection: Axis.horizontal,
                itemCount: similarProducts.length,
                itemBuilder: (context, index) {
                  final p = similarProducts[index];
                  return Padding(
                    padding: const EdgeInsets.only(right: 16.0),
                    child: SizedBox(
                      width: 165,
                      child: ProductCard(
                        product: p,
                        heroTag: 'similar-${p.id}-$index',
                      ),
                    ),
                  );
                },
              ),
            ),
          ],
        );
      },
    );
  }

  Widget _buildImageCarousel() {
    final images = _currentProduct.imageUrls.isNotEmpty
        ? _currentProduct.imageUrls
        : ['https://via.placeholder.com/600x400'];

    return Column(
      children: [
        Stack(
          children: [
            SizedBox(
              height: 300,
              child: PageView.builder(
                itemCount: images.length,
                onPageChanged: (index) =>
                    setState(() => _currentImageIndex = index),
                itemBuilder: (context, index) {
                  return Padding(
                    padding: const EdgeInsets.symmetric(horizontal: 20),
                    child: ClipRRect(
                      borderRadius: BorderRadius.circular(24),
                      child: Hero(
                        tag: widget.heroTag ?? 'product-${_currentProduct.id}',
                        child: RetryNetworkImage(
                          imageUrl: images[index],
                          fit: BoxFit.cover,
                          width: double.infinity,
                        ),
                      ),
                    ),
                  );
                },
              ),
            ),
            if (_currentProduct.reviewCount > 0)
              Positioned(
                top: 15,
                right: 35,
                child: Container(
                  padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
                  decoration: BoxDecoration(
                    color: Colors.white,
                    borderRadius: BorderRadius.circular(20),
                    boxShadow: [
                      BoxShadow(
                        color: Colors.black.withOpacity(0.1),
                        blurRadius: 8,
                        offset: const Offset(0, 2),
                      ),
                    ],
                  ),
                  child: Row(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      const Icon(Icons.star, color: Color(0xFFE8A020), size: 16),
                      const SizedBox(width: 4),
                      Text(
                        _currentProduct.averageRating.toStringAsFixed(1),
                        style: const TextStyle(
                          fontSize: 13,
                          fontWeight: FontWeight.bold,
                          color: Color(0xFF111827),
                        ),
                      ),
                    ],
                  ),
                ),
              ),
          ],
        ),
        const SizedBox(height: 16),
        Row(
          mainAxisAlignment: MainAxisAlignment.center,
          children: List.generate(
            images.length,
            (index) => Container(
              width: 8,
              height: 8,
              margin: const EdgeInsets.symmetric(horizontal: 4),
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                color: _currentImageIndex == index
                    ? const Color(0xFF0284C7)
                    : Colors.grey.shade300,
              ),
            ),
          ),
        ),
      ],
    );
  }

  Widget _buildSmallBadge(IconData icon, String label) {
    return Container(
      margin: const EdgeInsets.only(right: 8),
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 6),
      decoration: BoxDecoration(
        color: Colors.white,
        border: Border.all(color: Colors.grey.shade300),
        borderRadius: BorderRadius.circular(6),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(icon, size: 14, color: Colors.grey.shade600),
          const SizedBox(width: 4),
          Text(
            label,
            style: TextStyle(
              fontSize: 11,
              color: Colors.grey.shade600,
              fontWeight: FontWeight.w500,
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildActionButton({
    required IconData icon,
    required String label,
    required Color color,
    required VoidCallback onTap,
    bool isOutlined = false,
  }) {
    return GestureDetector(
      onTap: onTap,
      child: Container(
        height: 52,
        decoration: BoxDecoration(
          color: isOutlined ? Colors.white : color,
          borderRadius: BorderRadius.circular(12),
          border: isOutlined ? Border.all(color: color, width: 1.5) : null,
          boxShadow: isOutlined ? null : [
            BoxShadow(
              color: color.withOpacity(0.3),
              blurRadius: 12,
              offset: const Offset(0, 4),
            ),
          ],
        ),
        child: Row(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(icon, color: isOutlined ? color : Colors.white, size: 20),
            const SizedBox(width: 8),
            Text(
              label,
              style: TextStyle(
                color: isOutlined ? color : Colors.white,
                fontWeight: FontWeight.bold,
                fontSize: 16,
              ),
            ),
          ],
        ),
      ),
    );
  }

  TableRow _buildSpecRow(String label, String value) {
    return TableRow(
      children: [
        Padding(
          padding: const EdgeInsets.symmetric(vertical: 6.0),
          child: Text(
            label,
            style: TextStyle(
              fontWeight: FontWeight.bold,
              color: Colors.grey.shade800,
              fontSize: 14,
            ),
          ),
        ),
        Padding(
          padding: const EdgeInsets.symmetric(vertical: 6.0),
          child: Text(
            value,
            style: TextStyle(color: Colors.black87, fontSize: 14),
          ),
        ),
      ],
    );
  }

  Widget _buildBuyNowQuantitySelector(int quantityInCart) {
    final totalPrice = _currentProduct.price * _buyNowQuantity;
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
      decoration: BoxDecoration(
        color: Colors.grey.shade50,
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: Colors.grey.shade200),
      ),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const Text(
                "Quantity",
                style: TextStyle(
                  fontSize: 16,
                  fontWeight: FontWeight.bold,
                  color: Color(0xFF111827),
                ),
              ),
              const SizedBox(height: 4),
              Text(
                "Subtotal: ${_formatPrice(totalPrice)}",
                style: TextStyle(
                  fontSize: 13,
                  color: Colors.grey.shade600,
                  fontWeight: FontWeight.w600,
                ),
              ),
            ],
          ),
          Container(
            decoration: BoxDecoration(
              color: Colors.white,
              borderRadius: BorderRadius.circular(10),
              border: Border.all(color: Colors.grey.shade300),
              boxShadow: [
                BoxShadow(
                  color: Colors.black.withValues(alpha: 0.03),
                  blurRadius: 4,
                  offset: const Offset(0, 2),
                ),
              ],
            ),
            child: Row(
              children: [
                IconButton(
                  icon: Icon(
                    Icons.remove,
                    size: 18,
                    color: _buyNowQuantity > 1 ? Colors.black87 : Colors.grey.shade400,
                  ),
                  onPressed: _buyNowQuantity > 1
                      ? () async {
                          final newQuantity = _buyNowQuantity - 1;
                          if (quantityInCart > 0) {
                            try {
                              await context
                                  .read<CartProvider>()
                                  .updateCartQuantity(_currentProduct.id, newQuantity);
                            } catch (e) {
                              if (mounted) ErrorHandler.showError(context, e);
                            }
                          } else {
                            setState(() {
                              _buyNowQuantity = newQuantity;
                            });
                          }
                        }
                      : null,
                ),
                Text(
                  '$_buyNowQuantity',
                  style: const TextStyle(
                    fontSize: 16,
                    fontWeight: FontWeight.bold,
                    color: Color(0xFF111827),
                  ),
                ),
                IconButton(
                  icon: const Icon(
                    Icons.add,
                    size: 18,
                    color: Colors.black87,
                  ),
                  onPressed: () async {
                    final newQuantity = _buyNowQuantity + 1;
                    if (_currentProduct.manageStock && newQuantity > _currentProduct.stockQuantity) {
                      if (mounted) {
                        ScaffoldMessenger.of(context).showSnackBar(
                          SnackBar(
                            content: Text("Only ${_currentProduct.stockQuantity} units are available in stock."),
                            behavior: SnackBarBehavior.floating,
                            backgroundColor: Colors.red,
                          ),
                        );
                      }
                      return;
                    }
                    if (quantityInCart > 0) {
                      try {
                        await context
                            .read<CartProvider>()
                            .updateCartQuantity(_currentProduct.id, newQuantity);
                      } catch (e) {
                        if (mounted) ErrorHandler.showError(context, e);
                      }
                    } else {
                      setState(() {
                        _buyNowQuantity = newQuantity;
                      });
                    }
                  },
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
