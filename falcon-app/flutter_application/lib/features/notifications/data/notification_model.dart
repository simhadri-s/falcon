class AppNotification {
  final String id;
  final String? userId;
  final String title;
  final String body;
  final String? imageUrl;
  final Map<String, dynamic> data;
  final bool read;
  final DateTime createdAt;

  AppNotification({
    required this.id,
    this.userId,
    required this.title,
    required this.body,
    this.imageUrl,
    required this.data,
    required this.read,
    required this.createdAt,
  });

  factory AppNotification.fromJson(Map<String, dynamic> json) {
    return AppNotification(
      id: json['id'] ?? '',
      userId: json['userId'],
      title: json['title'] ?? '',
      body: json['body'] ?? '',
      imageUrl: json['imageUrl'],
      data: Map<String, dynamic>.from(json['data'] ?? {}),
      read: json['read'] ?? false,
      createdAt: json['createdAt'] != null 
          ? DateTime.parse(json['createdAt']) 
          : DateTime.now(),
    );
  }

  bool get isPersonal {
    final type = data['type'];
    if (type == 'ORDER_UPDATE') return true;
    if (userId != null && userId!.isNotEmpty) return true;
    if (type == 'ANNOUNCEMENT' || type == 'OFFER') return false;
    return userId != null && userId!.isNotEmpty;
  }
}
