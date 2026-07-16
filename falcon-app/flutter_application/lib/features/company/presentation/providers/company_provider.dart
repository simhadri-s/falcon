import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';
import '../../data/company_api_service.dart';
import '../../data/company_models.dart';

class CompanyProvider extends ChangeNotifier {
  final CompanyApiService _apiService = CompanyApiService();
  
  static const String _companyImageKey = 'company_image_data';
  static const String _companySettingsKey = 'company_settings_data';

  CompanyImage? _companyImage;
  CompanySettings? _companySettings;

  String get companyName => _companySettings?.companyName.isNotEmpty == true ? _companySettings!.companyName : (_companyImage?.name ?? 'Falcon Store');
  String get companyDescription => _companyImage?.description ?? '';
  String get logoUrl => _companyImage?.logoUrl ?? '';
  String get iconUrl => _companyImage?.iconUrl ?? '';
  String get faviconUrl => _companyImage?.faviconUrl ?? '';
  String get landingPageImageUrl => _companyImage?.landingPageImageUrl ?? '';
  
  String get email => _companySettings?.email ?? '';
  String get phone => _companySettings?.phone ?? '';
  String get address => _companySettings?.address ?? '';
  String get workingHours => _companySettings?.workingHours ?? '';
  String get termsAndConditions => _companySettings?.termsAndConditions ?? '';
  String get privacyPolicy => _companySettings?.privacyPolicy ?? '';

  CompanyProvider() {
    _init();
  }

  Future<void>? _apiFetchFuture;
  Future<void>? get apiFetchFuture => _apiFetchFuture;

  Future<void> _init() async {
    await _loadFromCache();
    // Fetch latest from API in background
    _apiFetchFuture = _fetchFromApi();
  }

  Future<void> _loadFromCache() async {
    final prefs = await SharedPreferences.getInstance();
    
    final imageJsonStr = prefs.getString(_companyImageKey);
    if (imageJsonStr != null) {
      try {
        _companyImage = CompanyImage.fromJson(json.decode(imageJsonStr));
      } catch (e) {
        debugPrint('Error parsing cached company image: $e');
      }
    }

    final settingsJsonStr = prefs.getString(_companySettingsKey);
    if (settingsJsonStr != null) {
      try {
        _companySettings = CompanySettings.fromJson(json.decode(settingsJsonStr));
      } catch (e) {
        debugPrint('Error parsing cached company settings: $e');
      }
    }

    notifyListeners();
  }

  Future<void> _fetchFromApi() async {
    final images = await _apiService.getCompanyImages();
    if (images.isNotEmpty) {
      _companyImage = images.first; // Assuming the first one is the main logo
      
      final prefs = await SharedPreferences.getInstance();
      await prefs.setString(_companyImageKey, json.encode(_companyImage!.toJson()));
    }

    final fetchedSettings = await _apiService.getCompanySettings();
    if (fetchedSettings != null) {
      _companySettings = fetchedSettings;
      
      final prefs = await SharedPreferences.getInstance();
      await prefs.setString(_companySettingsKey, json.encode(_companySettings!.toJson()));
    }

    notifyListeners();
  }
}
