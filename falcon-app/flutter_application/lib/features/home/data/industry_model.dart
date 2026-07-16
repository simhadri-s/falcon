class Industry {
  final String id;
  final String name;
  final String slug;
  final String? iconUrl;
  final String description;

  Industry({
    required this.id,
    required this.name,
    required this.slug,
    this.iconUrl,
    required this.description,
  });

  factory Industry.fromJson(Map<String, dynamic> json) {
    return Industry(
      id: json['id'] ?? '',
      name: json['name'] ?? '',
      slug: json['slug'] ?? '',
      iconUrl: json['iconUrl'],
      description: json['description'] ?? '',
    );
  }
}
