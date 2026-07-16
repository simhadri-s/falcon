import 'dart:convert';
import 'package:flutter_application/core/network/api_client.dart';
import 'product_model.dart';
import 'news_model.dart';
import 'banner_model.dart';
import 'industry_model.dart';
import 'package:flutter_application/core/error/api_exception.dart';
import 'package:flutter_application/core/constants/api_constants.dart';

class PaginatedNews {
  final List<News> news;
  final int total;
  final int pages;

  PaginatedNews({
    required this.news,
    required this.total,
    required this.pages,
  });
}

class HomeApiService {
  static String get baseUrl => ApiConstants.baseUrl;

  Future<List<BannerModel>> getBanners() async {
    try {
      final response = await ApiClient.get(Uri.parse('$baseUrl/banners'));
      if (response.statusCode == 200) {
        final List<dynamic> data = json.decode(response.body);
        final List<BannerModel> banners = data.map((json) => BannerModel.fromJson(json)).toList();
        
        // Sort so the default banner appears first
        banners.sort((a, b) {
          if (a.defaultBanner && !b.defaultBanner) return -1;
          if (!a.defaultBanner && b.defaultBanner) return 1;
          return 0;
        });
        
        return banners;
      } else {
        throw ApiException.fromResponse(response);
      }
    } catch (e) {
      if (e is ApiException) rethrow;
      throw ApiException('Failed to load banners: $e');
    }
  }

  Future<List<Product>> getFeaturedProducts() async {
    try {
      final response = await ApiClient.get(Uri.parse('$baseUrl/products/featured'));
      if (response.statusCode == 200) {
        final Map<String, dynamic> body = json.decode(response.body);
        final List<dynamic> data = body['data'] ?? [];
        return data.map((json) => Product.fromJson(json)).toList();
      } else {
        throw ApiException.fromResponse(response);
      }
    } catch (e) {
      if (e is ApiException) rethrow;
      throw ApiException('Failed to load products: $e');
    }
  }

  Future<List<Product>> getOffers() async {
    try {
      final response = await ApiClient.get(Uri.parse('$baseUrl/offers'));
      if (response.statusCode == 200) {
        final List<dynamic> data = json.decode(response.body);
        return data.map((json) => Product.fromJson(json)).toList();
      } else {
        throw ApiException.fromResponse(response);
      }
    } catch (e) {
      if (e is ApiException) rethrow;
      throw ApiException('Failed to load offers: $e');
    }
  }

  Future<PaginatedNews> getNews({
    int page = 1,
    int size = 10,
    String? search,
  }) async {
    try {
      final queryParameters = {
        'page': page.toString(),
        'size': size.toString(),
        if (search != null && search.isNotEmpty) 'search': search,
      };

      final uri = Uri.parse('$baseUrl/news').replace(queryParameters: queryParameters);
      final response = await ApiClient.get(uri);

      if (response.statusCode == 200) {
        final Map<String, dynamic> body = json.decode(response.body);
        final List<dynamic> data = body['data'] ?? [];
        final int total = body['total'] ?? 0;
        final int pages = body['pages'] ?? 1;

        return PaginatedNews(
          news: data.map((json) => News.fromJson(json)).toList(),
          total: total,
          pages: pages,
        );
      } else {
        throw ApiException.fromResponse(response);
      }
    } catch (e) {
      if (e is ApiException) rethrow;
      throw ApiException('Failed to load news: $e');
    }
  }

  Future<List<Industry>> getIndustries() async {
    try {
      final response = await ApiClient.get(Uri.parse('$baseUrl/industries'));
      if (response.statusCode == 200) {
        final List<dynamic> data = json.decode(response.body);
        return data.map((json) => Industry.fromJson(json)).toList();
      } else {
        throw ApiException.fromResponse(response);
      }
    } catch (e) {
      if (e is ApiException) rethrow;
      throw ApiException('Failed to load industries: $e');
    }
  }
}
