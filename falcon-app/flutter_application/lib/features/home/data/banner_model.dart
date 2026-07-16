class BannerModel {
  final String id;
  final String title;
  final String description;
  final String imageUrl;
  final bool active;
  final bool defaultBanner;

  BannerModel({
    required this.id,
    required this.title,
    required this.description,
    required this.imageUrl,
    required this.active,
    required this.defaultBanner,
  });

  factory BannerModel.fromJson(Map<String, dynamic> json) {
    return BannerModel(
      id: json['id'] ?? '',
      title: json['title'] ?? '',
      description: json['description'] ?? json['Description'] ?? '',
      imageUrl: json['imageUrl'] ?? (json['imageUrls'] != null && json['imageUrls'].isNotEmpty ? json['imageUrls'][0] : ''),
      active: json['active'] ?? false,
      defaultBanner: json['defaultBanner'] ?? false,
    );
  }
}