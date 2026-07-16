class News {
  final String id;
  final String title;
  final String slug;
  final String content;
  final String category;
  final List<String> imageUrls;
  final DateTime? createdAt;

  News({
    required this.id,
    required this.title,
    required this.slug,
    required this.content,
    required this.category,
    required this.imageUrls,
    this.createdAt,
  });

  factory News.fromJson(Map<String, dynamic> json) {
    return News(
      id: json['id'] ?? '',
      title: json['title'] ?? '',
      slug: json['slug'] ?? '',
      content: json['content'] ?? '',
      category: json['category'] ?? '',
      imageUrls: List<String>.from(json['imageUrls'] ?? []),
      createdAt: json['createdAt'] != null ? DateTime.tryParse(json['createdAt']) : null,
    );
  }
}