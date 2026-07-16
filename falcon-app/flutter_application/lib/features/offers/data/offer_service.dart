import 'dart:convert';
import 'package:flutter_application/core/constants/api_constants.dart';
import 'package:flutter_application/core/network/api_client.dart';
import 'offer_model.dart';

class OfferService {

  Future<List<Offer>> getAllOffers() async {
  final response = await ApiClient.get(
    Uri.parse('${ApiConstants.baseUrl}/offers'),
  );

  if (response.statusCode == 200) {
    final List<dynamic> data = json.decode(response.body);
    return data.map((json) => Offer.fromJson(json)).toList();
  } else {
    throw Exception('Failed to load offers');
  }
}

  Future<Offer> createOffer(Offer offer) async {
    final response = await ApiClient.post(
      Uri.parse('${ApiConstants.baseUrl}/offers'),
      body: json.encode(offer.toJson()),
    );

    if (response.statusCode == 200 || response.statusCode == 201) {
      return Offer.fromJson(json.decode(response.body));
    } else {
      throw Exception('Failed to create offer: ${response.body}');
    }
  }

  Future<Offer> updateOffer(Offer offer) async {
    final response = await ApiClient.put(
      Uri.parse('${ApiConstants.baseUrl}/offers/${offer.id}'),
      body: json.encode(offer.toJson()),
    );

    if (response.statusCode == 200) {
      return Offer.fromJson(json.decode(response.body));
    } else {
      throw Exception('Failed to update offer: ${response.body}');
    }
  }

  Future<void> deleteOffer(String id) async {
    final response = await ApiClient.delete(
      Uri.parse('${ApiConstants.baseUrl}/offers/$id'),
    );

    if (response.statusCode != 200 && response.statusCode != 204) {
      throw Exception('Failed to delete offer');
    }
  }
}
