import 'package:flutter/material.dart';
import 'package:flutter_application/features/products/presentation/pages/products_page.dart';
import '../widgets/header.dart';
import '../widgets/search_bar.dart';
import '../widgets/banner_widget.dart';
import '../widgets/section_header.dart';
import '../widgets/product_card.dart';
import 'package:flutter_application/features/home/data/product_model.dart';
import 'package:flutter_application/features/home/data/industry_model.dart';
import '../widgets/news_card.dart';
import '../widgets/industry_tile.dart';
import 'package:flutter_application/features/home/data/news_model.dart';
import 'news_page.dart';

import 'package:flutter_application/features/home/data/home_api_service.dart';
import 'package:flutter_application/features/home/data/banner_model.dart';
import 'package:flutter_application/features/products/data/products_api_service.dart';
import 'dart:async';
import 'package:flutter_application/core/widgets/app_drawer.dart';

class HomePage extends StatefulWidget {
  const HomePage({super.key});

  @override
  State<HomePage> createState() => _HomePageState();
}

class _HomePageState extends State<HomePage> {
  final HomeApiService _apiService = HomeApiService();
  final ProductsApiService _productsApiService = ProductsApiService();

  late Future<List<BannerModel>> _bannersFuture;
  late Future<List<Product>> _productsFuture;
  late Future<List<Product>> _offersFuture;
  late Future<List<Industry>> _industriesFuture;
  late Future<List<News>> _newsFuture;

  final TextEditingController _searchController = TextEditingController();
  String _searchQuery = "";
  bool _isSearching = false;
  Future<PaginatedProducts>? _searchProductsFuture;
  Future<PaginatedNews>? _searchNewsFuture;

  Timer? _debounce;

  @override
  void initState() {
    super.initState();
    _bannersFuture = _apiService.getBanners();
    _productsFuture = _apiService.getFeaturedProducts();
    _offersFuture = _apiService.getOffers();
    _industriesFuture = _apiService.getIndustries();
    _newsFuture = _apiService.getNews().then((p) => p.news);
  }

  @override
  void dispose() {
    _searchController.dispose();
    _debounce?.cancel();
    super.dispose();
  }

  Future<void> _onRefresh() async {
    setState(() {
      if (_isSearching) {
        _searchProductsFuture =
            _productsApiService.getAllProducts(search: _searchQuery);
        _searchNewsFuture = _apiService.getNews(search: _searchQuery);
      } else {
        _bannersFuture = _apiService.getBanners();
        _productsFuture = _apiService.getFeaturedProducts();
        _offersFuture = _apiService.getOffers();
        _industriesFuture = _apiService.getIndustries();
        _newsFuture = _apiService.getNews().then((p) => p.news);
      }
    });
    if (_isSearching) {
      await Future.wait([_searchProductsFuture!, _searchNewsFuture!]);
    } else {
      await Future.wait(
          [_bannersFuture, _productsFuture, _offersFuture, _industriesFuture, _newsFuture]);
    }
  }

  void _onSearchChanged(String query) {
    if (_debounce?.isActive ?? false) _debounce!.cancel();
    _debounce = Timer(const Duration(milliseconds: 500), () {
      setState(() {
        _searchQuery = query;
        _isSearching = query.isNotEmpty;
        if (_isSearching) {
          _searchProductsFuture =
              _productsApiService.getAllProducts(search: query);
          _searchNewsFuture = _apiService.getNews(search: query);
        }
      });
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      drawer: const AppDrawer(),
      backgroundColor: const Color(0xFFF9FAFB),
      body: SafeArea(
        bottom: false,
        child: RefreshIndicator(
          color: const Color(0xFF0284C7),
          onRefresh: _onRefresh,
          child: CustomScrollView(
            physics: const AlwaysScrollableScrollPhysics(),
            slivers: [
              // ── Sticky gradient header ─────────────────────────────
              const SliverToBoxAdapter(child: Header()),

              // ── Search bar ────────────────────────────────────────
              SliverToBoxAdapter(
                child: Container(
                  decoration: const BoxDecoration(
                    gradient: LinearGradient(
                      colors: [Color(0xFF0369A1), Color(0xFF0284C7)],
                      begin: Alignment.topLeft,
                      end: Alignment.bottomRight,
                    ),
                  ),
                  padding: const EdgeInsets.fromLTRB(16, 0, 16, 20),
                  child: CustomSearchBar(
                    controller: _searchController,
                    onChanged: _onSearchChanged,
                  ),
                ),
              ),

              if (_isSearching)
                ..._buildSearchResults()
              else ...[

                // ── Banner carousel ──────────────────────────────────
                SliverToBoxAdapter(
                  child: Padding(
                    padding: const EdgeInsets.fromLTRB(12, 4, 12, 0),
                    child: FutureBuilder<List<BannerModel>>(
                      future: _bannersFuture,
                      builder: (context, snapshot) {
                        if (snapshot.connectionState ==
                            ConnectionState.waiting) {
                          return _shimmerBox(height: 210);
                        } else if (snapshot.hasError) {
                          return _errorBox(
                              height: 210, message: "Error loading banners");
                        } else if (snapshot.hasData &&
                            snapshot.data!.isNotEmpty) {
                          return BannerWidget(banners: snapshot.data!);
                        } else {
                          return _emptyBox(
                              height: 210, message: "No banners available");
                        }
                      },
                    ),
                  ),
                ),
                


                // ── Industries multiple rows grid ─────────────────────
                SliverToBoxAdapter(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      const SizedBox(height: 24),
                      const Padding(
                        padding: EdgeInsets.symmetric(horizontal: 12),
                        child: SectionHeader(title: "Shop by Industry"),
                      ),
                      const SizedBox(height: 16),
                      FutureBuilder<List<Industry>>(
                        future: _industriesFuture,
                        builder: (context, snapshot) {
                          if (snapshot.connectionState ==
                              ConnectionState.waiting) {
                            return _shimmerBox(height: 110);
                          } else if (snapshot.hasData &&
                              snapshot.data!.isNotEmpty) {
                            return SizedBox(
                              height: 220, // Bounded height for horizontal GridView
                              child: GridView.builder(
                                scrollDirection: Axis.horizontal,
                                padding: const EdgeInsets.symmetric(horizontal: 16),
                                gridDelegate:
                                    const SliverGridDelegateWithFixedCrossAxisCount(
                                  crossAxisCount: 2, // 2 rows
                                  crossAxisSpacing: 16,
                                  mainAxisSpacing: 16,
                                  childAspectRatio: 1.1, // Adjusts width/height of the cells
                                ),
                                itemCount: snapshot.data!.length,
                                itemBuilder: (_, i) => IndustryTile(
                                  industry: snapshot.data![i],
                                  index: i,
                                  onTap: () => Navigator.push(
                                    context,
                                    MaterialPageRoute(
                                      builder: (context) => ProductsPage(
                                        industrySlug: snapshot.data![i].slug,
                                        industryName: snapshot.data![i].name,
                                      ),
                                    ),
                                  ),
                                ),
                              ),
                            );
                          } else {
                            return _emptyBox(
                                height: 110,
                                message: "No industries available");
                          }
                        },
                      ),
                    ],
                  ),
                ),

                // ── Special Offers ──────────────────────────────────
                SliverToBoxAdapter(
                  child: FutureBuilder<List<Product>>(
                    future: _offersFuture,
                    builder: (context, snapshot) {
                      if (snapshot.connectionState == ConnectionState.waiting) {
                        return const SizedBox.shrink();
                      }
                      if (snapshot.hasError) {
                        return Padding(
                          padding: const EdgeInsets.all(12.0),
                          child: Center(child: Text("Error loading offers: ${snapshot.error}", style: const TextStyle(fontSize: 10, color: Colors.red))),
                        );
                      }
                      if (snapshot.hasData && snapshot.data!.isNotEmpty) {
                        final items = snapshot.data!;
                        return Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            const SizedBox(height: 24),
                            Padding(
                              padding: const EdgeInsets.symmetric(horizontal: 12),
                              child: SectionHeader(
                                title: "Special Offers 🔥",
                                onTap: () => Navigator.push(
                                  context,
                                  MaterialPageRoute(
                                    builder: (context) => ProductsPage(
                                      // Note: showOffersOnly flag can be added to ProductsPage if needed
                                    ),
                                  ),
                                ),
                              ),
                            ),
                            const SizedBox(height: 16),
                            SizedBox(
                              height: 260,
                              child: ListView.builder(
                                scrollDirection: Axis.horizontal,
                                padding: const EdgeInsets.symmetric(horizontal: 12),
                                itemCount: items.length,
                                itemBuilder: (_, i) => Padding(
                                  padding: const EdgeInsets.only(right: 12),
                                  child: ProductCard(
                                    product: items[i],
                                    width: 170,
                                    heroTag: 'offer-${items[i].id}-$i',
                                  ),
                                ),
                              ),
                            ),
                          ],
                        );
                      }
                      return const SizedBox.shrink();
                    },
                  ),
                ),

                // ── Featured Products Grid ───────────────────────────
                SliverToBoxAdapter(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      const SizedBox(height: 28),
                      Padding(
                        padding:
                            const EdgeInsets.symmetric(horizontal: 12),
                        child: SectionHeader(
                          title: "Featured Products",
                          onTap: () => Navigator.push(
                            context,
                            MaterialPageRoute(
                                builder: (context) => ProductsPage()),
                          ),
                        ),
                      ),
                      const SizedBox(height: 14),
                    ],
                  ),
                ),

                SliverPadding(
                  padding: const EdgeInsets.symmetric(horizontal: 12),
                  sliver: FutureBuilder<List<Product>>(
                    future: _productsFuture,
                    builder: (context, snapshot) {
                      if (snapshot.connectionState ==
                          ConnectionState.waiting) {
                        return SliverToBoxAdapter(
                            child: _shimmerBox(height: 300));
                      } else if (snapshot.hasError) {
                        return SliverToBoxAdapter(
                            child: _errorBox(
                                height: 300,
                                message: "Error loading products"));
                      } else if (snapshot.hasData &&
                          snapshot.data!.isNotEmpty) {
                        final items = snapshot.data!;
                        return SliverGrid(
                          gridDelegate:
                              const SliverGridDelegateWithFixedCrossAxisCount(
                            crossAxisCount: 2,
                            crossAxisSpacing: 14,
                            mainAxisSpacing: 14,
                            childAspectRatio: 0.62,
                          ),
                          delegate: SliverChildBuilderDelegate(
                            (_, i) => ProductCard(
                              product: items[i],
                              heroTag: 'featured-${items[i].id}-$i',
                            ),
                            childCount: items.length,
                          ),
                        );
                      } else {
                        return SliverToBoxAdapter(
                            child: _emptyBox(
                                height: 200,
                                message: "No products available"));
                      }
                    },
                  ),
                ),

                // ── Latest News ─────────────────────────────────────
                SliverToBoxAdapter(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      const SizedBox(height: 28),
                      Padding(
                        padding:
                            const EdgeInsets.symmetric(horizontal: 12),
                        child: SectionHeader(
                          title: "Latest News",
                          onTap: () => Navigator.push(
                            context,
                            MaterialPageRoute(
                                builder: (context) => const NewsPage()),
                          ),
                        ),
                      ),
                      const SizedBox(height: 16),
                      FutureBuilder<List<News>>(
                        future: _newsFuture,
                        builder: (context, snapshot) {
                          if (snapshot.connectionState ==
                              ConnectionState.waiting) {
                            return _shimmerBox(height: 290);
                          } else if (snapshot.hasError) {
                            return _errorBox(
                                height: 200,
                                message: "Error loading news");
                          } else if (snapshot.hasData &&
                              snapshot.data!.isNotEmpty) {
                            return SizedBox(
                              height: 290,
                              child: ListView.builder(
                                scrollDirection: Axis.horizontal,
                                padding: const EdgeInsets.symmetric(
                                    horizontal: 12),
                                itemCount: snapshot.data!.length,
                                itemBuilder: (_, i) =>
                                    NewsCard(news: snapshot.data![i]),
                              ),
                            );
                          } else {
                            return _emptyBox(
                                height: 200,
                                message: "No news available");
                          }
                        },
                      ),
                      const SizedBox(height: 32),
                    ],
                  ),
                ),
              ],
            ],
          ),
        ),
      ),
    );
  }

  // ── Search results ───────────────────────────────────────────────
  List<Widget> _buildSearchResults() {
    return [
      // Products
      const SliverToBoxAdapter(
        child: Padding(
          padding: EdgeInsets.fromLTRB(12, 20, 12, 12),
          child: Row(
            children: [
              Icon(Icons.inventory_2_outlined,
                  size: 18, color: Color(0xFF4F46E5)),
              SizedBox(width: 8),
              Text(
                "Products",
                style: TextStyle(
                  fontSize: 17,
                  fontWeight: FontWeight.w700,
                  color: Color(0xFF1A1A2E),
                ),
              ),
            ],
          ),
        ),
      ),
      FutureBuilder<PaginatedProducts>(
        future: _searchProductsFuture,
        builder: (context, snapshot) {
          if (snapshot.connectionState == ConnectionState.waiting) {
            return SliverToBoxAdapter(child: _shimmerBox(height: 200));
          } else if (snapshot.hasError) {
            return SliverToBoxAdapter(
              child: _errorBox(
                  height: 100,
                  message: "Error searching products: ${snapshot.error}"),
            );
          } else if (!snapshot.hasData ||
              snapshot.data!.products.isEmpty) {
            return const SliverToBoxAdapter(
              child: Padding(
                padding: EdgeInsets.symmetric(horizontal: 12),
                child: Text("No products found"),
              ),
            );
          }
          return SliverPadding(
            padding: const EdgeInsets.symmetric(horizontal: 12),
            sliver: SliverGrid(
              gridDelegate:
                  const SliverGridDelegateWithFixedCrossAxisCount(
                crossAxisCount: 2,
                crossAxisSpacing: 14,
                mainAxisSpacing: 14,
                childAspectRatio: 0.62,
              ),
              delegate: SliverChildBuilderDelegate(
                (_, i) => ProductCard(
                  product: snapshot.data!.products[i],
                  heroTag: 'search-${snapshot.data!.products[i].id}-$i',
                ),
                childCount: snapshot.data!.products.length,
              ),
            ),
          );
        },
      ),

      const SliverToBoxAdapter(child: SizedBox(height: 28)),

      // News
      const SliverToBoxAdapter(
        child: Padding(
          padding: EdgeInsets.fromLTRB(12, 0, 12, 12),
          child: Row(
            children: [
              Icon(Icons.newspaper_outlined,
                  size: 18, color: Color(0xFF4F46E5)),
              SizedBox(width: 8),
              Text(
                "News",
                style: TextStyle(
                  fontSize: 17,
                  fontWeight: FontWeight.w700,
                  color: Color(0xFF1A1A2E),
                ),
              ),
            ],
          ),
        ),
      ),
      FutureBuilder<PaginatedNews>(
        future: _searchNewsFuture,
        builder: (context, snapshot) {
          if (snapshot.connectionState == ConnectionState.waiting) {
            return SliverToBoxAdapter(child: _shimmerBox(height: 200));
          } else if (snapshot.hasError) {
            return SliverToBoxAdapter(
              child: _errorBox(
                  height: 100,
                  message: "Error searching news: ${snapshot.error}"),
            );
          } else if (!snapshot.hasData || snapshot.data!.news.isEmpty) {
            return const SliverToBoxAdapter(
              child: Padding(
                padding: EdgeInsets.symmetric(horizontal: 12),
                child: Text("No news found"),
              ),
            );
          }
          return SliverPadding(
            padding: const EdgeInsets.symmetric(horizontal: 12),
            sliver: SliverList(
              delegate: SliverChildBuilderDelegate(
                (_, i) => NewsCard(
                    news: snapshot.data!.news[i], isHorizontal: false),
                childCount: snapshot.data!.news.length,
              ),
            ),
          );
        },
      ),
      const SliverToBoxAdapter(child: SizedBox(height: 50)),
    ];
  }

  // ── Utility builders ─────────────────────────────────────────────
  Widget _shimmerBox({required double height}) {
    return Container(
      height: height,
      margin: const EdgeInsets.symmetric(horizontal: 12),
      decoration: BoxDecoration(
        color: Colors.grey.shade200,
        borderRadius: BorderRadius.circular(16),
      ),
      child: const Center(
        child: CircularProgressIndicator(
          color: Color(0xFF4F46E5),
          strokeWidth: 2.5,
        ),
      ),
    );
  }

  Widget _errorBox({required double height, required String message}) {
    return Container(
      height: height,
      margin: const EdgeInsets.symmetric(horizontal: 12),
      decoration: BoxDecoration(
        color: const Color(0xFFFFEBEB),
        borderRadius: BorderRadius.circular(16),
      ),
      child: Center(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(Icons.wifi_off_outlined,
                color: Colors.red.shade300, size: 28),
            const SizedBox(height: 8),
            Text(
              message,
              style: TextStyle(color: Colors.red.shade400, fontSize: 13),
            ),
          ],
        ),
      ),
    );
  }

  Widget _emptyBox({required double height, required String message}) {
    return Container(
      height: height,
      margin: const EdgeInsets.symmetric(horizontal: 12),
      decoration: BoxDecoration(
        color: Colors.grey.shade50,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: Colors.grey.shade200),
      ),
      child: Center(
        child: Text(
          message,
          style: TextStyle(color: Colors.grey.shade400, fontSize: 13),
        ),
      ),
    );
  }
}