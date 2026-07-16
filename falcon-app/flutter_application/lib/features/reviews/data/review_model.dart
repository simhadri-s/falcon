class Review {
  final String id;
  final String productId;
  final String userId;
  final String userName;
  final int rating;
  final String comment;
  final String status;
  final DateTime createdAt;

  Review({
    required this.id,
    required this.productId,
    required this.userId,
    required this.userName,
    required this.rating,
    required this.comment,
    required this.status,
    required this.createdAt,
  });

  factory Review.fromJson(Map<String, dynamic> json) {
    return Review(
      id: json['id'] ?? '',
      productId: json['productId'] ?? '',
      userId: json['userId'] ?? '',
      userName: json['userName'] ?? 'Anonymous',
      rating: json['rating'] ?? 0,
      comment: json['comment'] ?? '',
      status: json['status'] ?? 'PENDING',
      createdAt: json['createdAt'] != null 
          ? DateTime.parse(json['createdAt']) 
          : DateTime.now(),
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'productId': productId,
      'userId': userId,
      'userName': userName,
      'rating': rating,
      'comment': comment,
      'status': status,
      'createdAt': createdAt.toIso8601String(),
    };
  }
}

class PaginatedReviews {
  final List<Review> reviews;
  final int total;
  final int pages;

  PaginatedReviews({
    required this.reviews,
    required this.total,
    required this.pages,
  });
}
