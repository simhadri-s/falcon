import 'dart:async';
import 'package:flutter/material.dart';
import '../widgets/product_grid_item.dart';
import 'package:flutter_application/features/home/data/product_model.dart';
import '../../data/products_api_service.dart';
import 'package:flutter_application/core/utils/error_handler.dart';
import 'package:flutter_application/core/widgets/app_drawer.dart';
import 'package:flutter_application/features/home/presentation/widgets/search_bar.dart';

class ProductsPage extends StatefulWidget {
  final String? industrySlug;
  final String? industryName;
  final String? categorySlug;
  final String? categoryName;

  const ProductsPage({
    super.key,
    this.industrySlug,
    this.industryName,
    this.categorySlug,
    this.categoryName,
  });

  @override
  State<ProductsPage> createState() => _ProductsPageState();
}

class _ProductsPageState extends State<ProductsPage> {
  final ProductsApiService _apiService = ProductsApiService();
  final ScrollController _scrollController = ScrollController();
  final TextEditingController _searchController = TextEditingController();

  List<Product> _products = [];
  bool _isLoading = false;
  bool _hasMore = true;
  int _currentPage = 1;
  final int _pageSize = 10;
  String? _selectedCategory;
  String? _selectedCategoryName;
  String? _selectedSubCategory;
  String? _selectedSubCategoryName;
  String _searchQuery = "";
  String? _sortBy;
  String? _sortDirection;
  Timer? _debounce;
  int _totalItems = 0;

  @override
  void initState() {
    super.initState();
    _selectedCategory = widget.categorySlug;
    _selectedCategoryName = widget.categoryName;
    _fetchProducts();
    _scrollController.addListener(_onScroll);
  }

  @override
  void dispose() {
    _scrollController.dispose();
    _searchController.dispose();
    _debounce?.cancel();
    super.dispose();
  }

  void _onScroll() {
    if (_scrollController.position.pixels >=
            _scrollController.position.maxScrollExtent - 200 &&
        !_isLoading &&
        _hasMore) {
      _fetchProducts();
    }
  }

  Future<void> _fetchProducts({bool reset = false}) async {
    if (reset) {
      setState(() {
        _currentPage = 1;
        _products = [];
        _hasMore = true;
      });
    }
    if (_isLoading || !_hasMore) return;
    setState(() => _isLoading = true);
    try {
      PaginatedProducts response;
      if (widget.industrySlug != null) {
        response = await _apiService.getProductsByIndustry(
          widget.industrySlug!,
          page: _currentPage,
          size: _pageSize,
          sortBy: _sortBy,
          sortDirection: _sortDirection,
        );
      } else {
        response = await _apiService.getAllProducts(
          page: _currentPage,
          size: _pageSize,
          category: _selectedCategory,
          subCategory: _selectedSubCategory,
          search: _searchQuery,
          sortBy: _sortBy,
          sortDirection: _sortDirection,
        );
      }
      setState(() {
        _isLoading = false;
        _totalItems = response.total;
        if (response.products.length < _pageSize ||
            _products.length + response.products.length >= response.total) {
          _hasMore = false;
        }
        _products.addAll(response.products);
        _currentPage++;
      });
    } catch (e) {
      setState(() => _isLoading = false);
      if (mounted) ErrorHandler.showError(context, e);
    }
  }

  void _showSortBottomSheet() {
    showModalBottomSheet(
      context: context,
      backgroundColor: Colors.white,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(16)),
      ),
      builder: (context) {
        final sortOptions = [
          {'label': 'Newest First', 'sortBy': 'createdAt', 'sortDirection': 'desc'},
          {'label': 'Price: Low to High', 'sortBy': 'sellingPrice', 'sortDirection': 'asc'},
          {'label': 'Price: High to Low', 'sortBy': 'sellingPrice', 'sortDirection': 'desc'},
          {'label': 'Name: A to Z', 'sortBy': 'name', 'sortDirection': 'asc'},
        ];

        return Container(
          padding: const EdgeInsets.fromLTRB(20, 12, 20, 24),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              // Drag handle
              Center(
                child: Container(
                  width: 36,
                  height: 4,
                  decoration: BoxDecoration(
                    color: Colors.grey.shade300,
                    borderRadius: BorderRadius.circular(2),
                  ),
                ),
              ),
              const SizedBox(height: 16),
              const Text(
                "Sort By",
                style: TextStyle(
                  fontSize: 16,
                  fontWeight: FontWeight.w700,
                  color: Color(0xFF1A1A2E),
                ),
              ),
              const SizedBox(height: 14),
              Wrap(
                spacing: 8,
                runSpacing: 8,
                children: sortOptions.map((option) {
                  final isSelected = _sortBy == option['sortBy'] && _sortDirection == option['sortDirection'];
                  return GestureDetector(
                    onTap: () {
                      setState(() {
                        _sortBy = option['sortBy'];
                        _sortDirection = option['sortDirection'];
                      });
                      Navigator.pop(context);
                      _fetchProducts(reset: true);
                    },
                    child: Container(
                      padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 8),
                      decoration: BoxDecoration(
                        color: isSelected ? const Color(0xFF4F46E5) : Colors.grey.shade100,
                        borderRadius: BorderRadius.circular(8),
                        border: Border.all(
                          color: isSelected ? const Color(0xFF4F46E5) : Colors.grey.shade300,
                        ),
                      ),
                      child: Text(
                        option['label']!,
                        style: TextStyle(
                          fontSize: 13,
                          fontWeight: FontWeight.w500,
                          color: isSelected ? Colors.white : const Color(0xFF1A1A2E),
                        ),
                      ),
                    ),
                  );
                }).toList(),
              ),
              const SizedBox(height: 16),
            ],
          ),
        );
      },
    );
  }

  void _showFilterBottomSheet() {
    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.white,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(20)),
      ),
      builder: (context) {
        return StatefulBuilder(builder: (context, setModalState) {
          return Container(
            padding: EdgeInsets.fromLTRB(20, 12, 20, MediaQuery.of(context).viewInsets.bottom + 24),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Center(
                  child: Container(
                    width: 36,
                    height: 4,
                    decoration: BoxDecoration(
                      color: Colors.grey.shade300,
                      borderRadius: BorderRadius.circular(2),
                    ),
                  ),
                ),
                const SizedBox(height: 16),
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    const Text(
                      "Filter Products",
                      style: TextStyle(fontSize: 18, fontWeight: FontWeight.w800, color: Color(0xFF1A1A2E)),
                    ),
                    if (_selectedCategory != null || _selectedSubCategory != null)
                      TextButton(
                        onPressed: () {
                          setState(() {
                            _selectedCategory = null;
                            _selectedCategoryName = null;
                            _selectedSubCategory = null;
                            _selectedSubCategoryName = null;
                          });
                          Navigator.pop(context);
                          _fetchProducts(reset: true);
                        },
                        child: const Text("Clear All", style: TextStyle(color: Color(0xFF4F46E5), fontWeight: FontWeight.w600)),
                      ),
                  ],
                ),
                const Divider(height: 24),
                const Text("Select Category", style: TextStyle(fontSize: 14, fontWeight: FontWeight.w700, color: Colors.grey)),
                const SizedBox(height: 12),
                FutureBuilder<List<Category>>(
                  future: _apiService.getCategories(),
                  builder: (context, snapshot) {
                    if (!snapshot.hasData) return const Center(child: LinearProgressIndicator());
                    final categories = snapshot.data!;
                    return Wrap(
                      spacing: 8,
                      runSpacing: 8,
                      children: categories.map((cat) {
                        final selected = _selectedCategory == cat.slug;
                        return GestureDetector(
                          onTap: () {
                            setModalState(() {
                              _selectedCategory = cat.slug;
                              _selectedCategoryName = cat.name;
                              _selectedSubCategory = null; // Reset sub when cat changes
                              _selectedSubCategoryName = null;
                            });
                            setState(() {}); // Trigger rebuild to update sub-category future
                          },
                          child: Container(
                            padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
                            decoration: BoxDecoration(
                              color: selected ? const Color(0xFF4F46E5) : Colors.white,
                              borderRadius: BorderRadius.circular(10),
                              border: Border.all(color: selected ? const Color(0xFF4F46E5) : Colors.grey.shade300),
                              boxShadow: selected ? [BoxShadow(color: const Color(0xFF4F46E5).withOpacity(0.3), blurRadius: 4, offset: const Offset(0, 2))] : null,
                            ),
                            child: Text(cat.name, style: TextStyle(fontSize: 13, fontWeight: FontWeight.w600, color: selected ? Colors.white : const Color(0xFF1A1A2E))),
                          ),
                        );
                      }).toList(),
                    );
                  },
                ),
                if (_selectedCategory != null) ...[
                  const SizedBox(height: 24),
                  const Text("Select Sub-Category", style: TextStyle(fontSize: 14, fontWeight: FontWeight.w700, color: Colors.grey)),
                  const SizedBox(height: 12),
                  FutureBuilder<List<Category>>(
                    future: _apiService.getSubCategories(_selectedCategory!),
                    builder: (context, snapshot) {
                      if (snapshot.connectionState == ConnectionState.waiting) return const Center(child: LinearProgressIndicator());
                      final subs = snapshot.data ?? [];
                      if (subs.isEmpty) return const Text("No sub-categories found", style: TextStyle(fontStyle: FontStyle.italic, color: Colors.grey));
                      return Wrap(
                        spacing: 8,
                        runSpacing: 8,
                        children: subs.map((sub) {
                          final selected = _selectedSubCategory == sub.slug;
                          return GestureDetector(
                            onTap: () {
                              setModalState(() {
                                _selectedSubCategory = sub.slug;
                                _selectedSubCategoryName = sub.name;
                              });
                              setState(() {});
                            },
                            child: Container(
                              padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
                              decoration: BoxDecoration(
                                color: selected ? const Color(0xFFE8A020) : Colors.white,
                                borderRadius: BorderRadius.circular(10),
                                border: Border.all(color: selected ? const Color(0xFFE8A020) : Colors.grey.shade300),
                              ),
                              child: Text(sub.name, style: TextStyle(fontSize: 13, fontWeight: FontWeight.w600, color: selected ? Colors.white : const Color(0xFF1A1A2E))),
                            ),
                          );
                        }).toList(),
                      );
                    },
                  ),
                ],
                const SizedBox(height: 32),
                SizedBox(
                  width: double.infinity,
                  height: 50,
                  child: ElevatedButton(
                    onPressed: () {
                      Navigator.pop(context);
                      _fetchProducts(reset: true);
                    },
                    style: ElevatedButton.styleFrom(
                      backgroundColor: const Color(0xFF4F46E5),
                      foregroundColor: Colors.white,
                      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                      elevation: 0,
                    ),
                    child: const Text("Apply Filters", style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
                  ),
                ),
                const SizedBox(height: 10),
              ],
            ),
          );
        });
      },
    );
  }

  @override
  Widget build(BuildContext context) {
    final String pageTitle = widget.industryName ??
        widget.categoryName ??
        "All Products";

    // Is this page opened as a standalone push (has back button) or as a tab?
    final bool isTab = widget.industrySlug == null &&
        widget.industryName == null &&
        widget.categorySlug == null &&
        widget.categoryName == null;

    return Scaffold(
      drawer: isTab ? const AppDrawer() : null,
      backgroundColor: const Color(0xFFF8F7FF),
      body: Column(
        children: [
          // ── Top App Bar ──────────────────────────────────────────
          Container(
            decoration: const BoxDecoration(
              gradient: LinearGradient(
                colors: [Color(0xFF4F46E5), Color(0xFF7C3AED)],
                begin: Alignment.topLeft,
                end: Alignment.bottomRight,
              ),
            ),
            child: SafeArea(
              bottom: false,
              child: Column(
                children: [
                  // Title row
                  Padding(
                    padding: const EdgeInsets.fromLTRB(8, 8, 16, 0),
                    child: Row(
                      children: [
                        if (!isTab)
                          IconButton(
                            icon: const Icon(Icons.arrow_back_ios_new,
                                color: Colors.white, size: 20),
                            onPressed: () => Navigator.pop(context),
                          )
                        else
                          Builder(
                            builder: (ctx) => IconButton(
                              icon: const Icon(Icons.menu,
                                  color: Colors.white, size: 22),
                              onPressed: () =>
                                  Scaffold.of(ctx).openDrawer(),
                            ),
                          ),
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text(
                                pageTitle,
                                style: const TextStyle(
                                  color: Colors.white,
                                  fontSize: 18,
                                  fontWeight: FontWeight.w700,
                                  letterSpacing: -0.3,
                                ),
                                maxLines: 1,
                                overflow: TextOverflow.ellipsis,
                              ),
                              if (_totalItems > 0)
                                Text(
                                  '$_totalItems items found',
                                  style: const TextStyle(
                                    color: Colors.white60,
                                    fontSize: 11,
                                  ),
                                ),
                            ],
                          ),
                        ),
                        Row(
                          mainAxisSize: MainAxisSize.min,
                          children: [
                            GestureDetector(
                              onTap: _showSortBottomSheet,
                              child: Container(
                                padding: const EdgeInsets.symmetric(
                                    horizontal: 14, vertical: 8),
                                decoration: BoxDecoration(
                                  color: Colors.white.withValues(alpha: 0.18),
                                  borderRadius: BorderRadius.circular(8),
                                  border: Border.all(
                                      color:
                                          Colors.white.withValues(alpha: 0.3)),
                                ),
                                child: const Row(
                                  mainAxisSize: MainAxisSize.min,
                                  children: [
                                    Icon(Icons.sort,
                                        color: Colors.white, size: 16),
                                    SizedBox(width: 5),
                                    Text(
                                      "Sort",
                                      style: TextStyle(
                                        color: Colors.white,
                                        fontSize: 12,
                                        fontWeight: FontWeight.w600,
                                      ),
                                    ),
                                  ],
                                ),
                              ),
                            ),
                            const SizedBox(width: 8),
                            if (widget.industrySlug == null)
                              GestureDetector(
                                onTap: _showFilterBottomSheet,
                                child: Container(
                                  padding: const EdgeInsets.symmetric(
                                      horizontal: 14, vertical: 8),
                                  decoration: BoxDecoration(
                                    color: Colors.white.withValues(alpha: 0.18),
                                    borderRadius: BorderRadius.circular(8),
                                    border: Border.all(
                                        color:
                                            Colors.white.withValues(alpha: 0.3)),
                                  ),
                                  child: const Row(
                                    mainAxisSize: MainAxisSize.min,
                                    children: [
                                      Icon(Icons.filter_list,
                                          color: Colors.white, size: 16),
                                      SizedBox(width: 5),
                                      Text(
                                        "Filter",
                                        style: TextStyle(
                                          color: Colors.white,
                                          fontSize: 12,
                                          fontWeight: FontWeight.w600,
                                        ),
                                      ),
                                    ],
                                  ),
                                ),
                              ),
                          ],
                        ),
                      ],
                    ),
                  ),
                  // Filter Status Bar
                  if (_selectedCategory != null || _selectedSubCategory != null)
                    Padding(
                      padding: const EdgeInsets.fromLTRB(16, 0, 16, 12),
                      child: Container(
                        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
                        decoration: BoxDecoration(
                          color: Colors.white.withOpacity(0.15),
                          borderRadius: BorderRadius.circular(10),
                        ),
                        child: Row(
                          children: [
                            const Icon(Icons.info_outline, color: Colors.white70, size: 14),
                            const SizedBox(width: 8),
                            Expanded(
                              child: RichText(
                                text: TextSpan(
                                  style: const TextStyle(color: Colors.white, fontSize: 12),
                                  children: [
                                    const TextSpan(text: "Showing: ", style: TextStyle(color: Colors.white70)),
                                    TextSpan(text: _selectedCategoryName ?? "Category", style: const TextStyle(fontWeight: FontWeight.bold)),
                                    if (_selectedSubCategory != null) ...[
                                      const TextSpan(text: "  ›  ", style: TextStyle(color: Colors.white60)),
                                      TextSpan(text: _selectedSubCategoryName ?? "Sub-category", style: const TextStyle(fontWeight: FontWeight.bold, color: Color(0xFFE8A020))),
                                    ],
                                  ],
                                ),
                              ),
                            ),
                            GestureDetector(
                              onTap: () {
                                setState(() {
                                  _selectedCategory = null;
                                  _selectedCategoryName = null;
                                  _selectedSubCategory = null;
                                  _selectedSubCategoryName = null;
                                });
                                _fetchProducts(reset: true);
                              },
                              child: const Icon(Icons.close, color: Colors.white70, size: 16),
                            ),
                          ],
                        ),
                      ),
                    ),
                  // Search bar inside header
                  Padding(
                    padding: const EdgeInsets.fromLTRB(16, 10, 16, 14),
                    child: CustomSearchBar(
                      controller: _searchController,
                      hintText: "Search products...",
                      onChanged: (value) {
                        if (_debounce?.isActive ?? false) {
                          _debounce!.cancel();
                        }
                        _debounce = Timer(
                            const Duration(milliseconds: 500), () {
                          setState(() => _searchQuery = value);
                          _fetchProducts(reset: true);
                        });
                      },
                    ),
                  ),
                ],
              ),
            ),
          ),

          // ── Product Grid ─────────────────────────────────────────
          Expanded(
            child: RefreshIndicator(
              color: const Color(0xFF4F46E5),
              onRefresh: () => _fetchProducts(reset: true),
              child: _products.isEmpty && _isLoading
                  ? const Center(
                      child: CircularProgressIndicator(
                          color: Color(0xFF4F46E5)))
                  : _products.isEmpty
                      ? ListView(
                          physics: const AlwaysScrollableScrollPhysics(),
                          children: [
                            SizedBox(
                              height: MediaQuery.of(context).size.height * 0.25,
                            ),
                            Center(
                              child: Column(
                                mainAxisSize: MainAxisSize.min,
                                children: [
                                  Icon(Icons.inventory_2_outlined,
                                      size: 56, color: Colors.grey.shade300),
                                  const SizedBox(height: 12),
                                  Text(
                                    "No products found",
                                    style: TextStyle(
                                        color: Colors.grey.shade500,
                                        fontSize: 15),
                                  ),
                                ],
                              ),
                            ),
                          ],
                        )
                      : GridView.builder(
                          controller: _scrollController,
                          physics:
                              const AlwaysScrollableScrollPhysics(),
                          padding: const EdgeInsets.fromLTRB(
                              14, 14, 14, 20),
                          gridDelegate:
                              const SliverGridDelegateWithFixedCrossAxisCount(
                            crossAxisCount: 2,
                            childAspectRatio: 0.65,
                            crossAxisSpacing: 12,
                            mainAxisSpacing: 14,
                          ),
                          itemCount:
                              _products.length + (_hasMore ? 1 : 0),
                          itemBuilder: (context, index) {
                            if (index == _products.length) {
                              return const Center(
                                  child: CircularProgressIndicator(
                                      color: Color(0xFF4F46E5)));
                            }
                            return ProductGridItem(
                              product: _products[index],
                              heroTag: 'products-${_products[index].id}-$index',
                            );
                          },
                        ),
            ),
          ),
        ],
      ),
    );
  }
}
