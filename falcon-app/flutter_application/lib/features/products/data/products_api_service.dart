import 'dart:convert';
import 'package:flutter/foundation.dart';
import 'package:flutter_application/core/network/api_client.dart';
import 'package:flutter_application/features/home/data/product_model.dart';
import 'package:flutter_application/core/error/api_exception.dart';
import 'package:flutter_application/core/constants/api_constants.dart';

class PaginatedProducts {
  final List<Product> products;
  final int total;
  final int pages;

  PaginatedProducts({
    required this.products,
    required this.total,
    required this.pages,
  });
}

class Category {
  final String name;
  final String slug;
  final String? imageUrl;

  Category({required this.name, required this.slug, this.imageUrl});
}

class ProductsApiService {
  static String get baseUrl => ApiConstants.baseUrl;

  Future<PaginatedProducts> getAllProducts({
    int page = 1,
    int size = 10,
    String? category,
    String? subCategory,
    String? search,
    String? sortBy,
    String? sortDirection,
  }) async {
    try {
      final queryParameters = {
        'page': page.toString(),
        'size': size.toString(),
        if (category != null && category.isNotEmpty) 'category': category,
        if (subCategory != null && subCategory.isNotEmpty) 'subCategory': subCategory,
        if (search != null && search.isNotEmpty) 'search': search,
        if (sortBy != null && sortBy.isNotEmpty) 'sortBy': sortBy,
        if (sortDirection != null && sortDirection.isNotEmpty) 'sortDirection': sortDirection,
      };

      final uri = Uri.parse('$baseUrl/products').replace(queryParameters: queryParameters);
      final response = await ApiClient.get(uri);

      if (response.statusCode == 200) {
        final Map<String, dynamic> body = json.decode(response.body);
        final List<dynamic> data = body['data'] ?? [];
        final int total = body['total'] ?? 0;
        final int pages = body['pages'] ?? 1;
        
        return PaginatedProducts(
          products: data.map((json) => Product.fromJson(json)).toList(),
          total: total,
          pages: pages,
        );
      } else {
        throw ApiException.fromResponse(response);
      }
    } catch (e) {
      if (e is ApiException) rethrow;
      throw ApiException('Failed to load products: $e');
    }
  }

  Future<PaginatedProducts> getProductsByIndustry(
    String slug, {
    int page = 1,
    int size = 10,
    String? sortBy,
    String? sortDirection,
  }) async {
    try {
      final queryParameters = {
        'page': page.toString(),
        'size': size.toString(),
        if (sortBy != null && sortBy.isNotEmpty) 'sortBy': sortBy,
        if (sortDirection != null && sortDirection.isNotEmpty) 'sortDirection': sortDirection,
      };

      final uri = Uri.parse('$baseUrl/industries/$slug/products').replace(queryParameters: queryParameters);
      final response = await ApiClient.get(uri);

      if (response.statusCode == 200) {
        final Map<String, dynamic> body = json.decode(response.body);
        final List<dynamic> data = body['data'] ?? [];
        final int total = body['total'] ?? 0;
        final int pages = body['pages'] ?? 1;

        return PaginatedProducts(
          products: data.map((json) => Product.fromJson(json)).toList(),
          total: total,
          pages: pages,
        );
      } else {
        throw ApiException.fromResponse(response);
      }
    } catch (e) {
      if (e is ApiException) rethrow;
      throw ApiException('Failed to load products for industry $slug: $e');
    }
  }

  Future<Product?> getProductById(String id) async {
    try {
      debugPrint('ProductsApiService: Fetching product by ID: $id');
      final response = await ApiClient.get(Uri.parse('$baseUrl/products/id/$id'));
      debugPrint('ProductsApiService: getProductById status: ${response.statusCode}');
      debugPrint('ProductsApiService: getProductById body: ${response.body}');
      
      if (response.statusCode == 200) {
        final dynamic decoded = json.decode(response.body);
        
        Map<String, dynamic> productJson;
        if (decoded is Map<String, dynamic>) {
          // Check if wrapped in 'data' key
          if (decoded.containsKey('data') && decoded['data'] is Map) {
            productJson = decoded['data'] as Map<String, dynamic>;
          } else {
            productJson = decoded;
          }
        } else {
          debugPrint('ProductsApiService: Unexpected response type: ${decoded.runtimeType}');
          return null;
        }
        
        final product = Product.fromJson(productJson);
        debugPrint('ProductsApiService: Parsed product: ${product.name}, images: ${product.imageUrls}');
        return product;
      }
      return null;
    } catch (e) {
      debugPrint('ProductsApiService: Error fetching product by ID $id: $e');
      return null;
    }
  }

  Future<List<Category>> getCategories() async {
    try {
      final response = await ApiClient.get(Uri.parse('$baseUrl/categories/products'));
      if (response.statusCode == 200) {
        final dynamic decoded = json.decode(response.body);
        List<dynamic> list;
        
        if (decoded is List) {
          list = decoded;
        } else if (decoded is Map && decoded.containsKey('data')) {
          list = decoded['data'] as List;
        } else {
          return [];
        }

        return list.map((item) {
          if (item is String) {
            final trimmed = item.trim();
            return Category(name: trimmed, slug: trimmed);
          }
          if (item is Map) {
            final name = (item['name'] ?? item['title'] ?? item.toString()).toString().trim();
            // Prioritize slug or name over ID for the filter parameter
            final slug = (item['slug'] ?? item['name'] ?? item['title'] ?? item['id'] ?? item.toString()).toString().trim().toLowerCase();
            final imageUrl = item['imageUrl']?.toString();
            return Category(name: name, slug: slug, imageUrl: imageUrl);
          }
          final val = item.toString().trim();
          return Category(name: val, slug: val.toLowerCase());
        }).toList();
      }
      return [];
    } catch (e) {
      return [];
    }
  }
  Future<List<Category>> getSubCategories(String categoryId) async {
    try {
      final url = '$baseUrl/categories/sub/category/$categoryId';
      debugPrint('ProductsApiService: Fetching sub-categories from $url');
      final response = await ApiClient.get(Uri.parse(url));
      
      if (response.statusCode == 200) {
        final dynamic decoded = json.decode(response.body);
        List<dynamic> list;
        
        if (decoded is List) {
          list = decoded;
        } else if (decoded is Map && decoded.containsKey('data')) {
          list = decoded['data'] as List;
        } else {
          debugPrint('ProductsApiService: Unexpected sub-category response structure');
          return [];
        }

        final result = list.map((item) {
          final name = (item['name'] ?? '').toString();
          final id = (item['id'] ?? item['_id'] ?? '').toString();
          final imageUrl = item['imageUrl']?.toString();
          return Category(name: name, slug: id, imageUrl: imageUrl);
        }).toList();
        
        debugPrint('ProductsApiService: Found ${result.length} sub-categories');
        return result;
      }
      debugPrint('ProductsApiService: Sub-category fetch failed with status ${response.statusCode}');
      return [];
    } catch (e) {
      debugPrint('ProductsApiService: Error fetching sub-categories: $e');
      return [];
    }
  }
}
