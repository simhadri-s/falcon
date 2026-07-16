import 'package:flutter/material.dart';
import 'package:flutter_application/features/reviews/data/review_model.dart';
import 'package:flutter_application/features/reviews/data/reviews_api_service.dart';
import 'package:intl/intl.dart';

class ReviewListWidget extends StatefulWidget {
  final String productId;
  final Key? listKey; // Add key parameter to force rebuild

  const ReviewListWidget({
    super.key,
    required this.productId,
    this.listKey,
  });

  @override
  State<ReviewListWidget> createState() => _ReviewListWidgetState();
}

class _ReviewListWidgetState extends State<ReviewListWidget> {
  final ReviewsApiService _apiService = ReviewsApiService();
  final List<Review> _reviews = [];
  bool _isLoading = true;
  bool _hasMore = true;
  int _currentPage = 1;
  String? _errorMessage;

  @override
  void initState() {
    super.initState();
    _fetchReviews();
  }

  // Reload when the key changes (e.g., after a new review is submitted)
  @override
  void didUpdateWidget(ReviewListWidget oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.listKey != widget.listKey) {
      _currentPage = 1;
      _reviews.clear();
      _hasMore = true;
      _fetchReviews();
    }
  }

  Future<void> _fetchReviews() async {
    if (!_hasMore) return;

    setState(() {
      _isLoading = true;
      _errorMessage = null;
    });

    try {
      final paginatedReviews = await _apiService.getProductReviews(
        widget.productId,
        page: _currentPage,
      );

      if (mounted) {
        setState(() {
          _reviews.addAll(paginatedReviews.reviews);
          _currentPage++;
          _hasMore = _currentPage <= paginatedReviews.pages;
          _isLoading = false;
        });
      }
    } catch (e) {
      if (mounted) {
        setState(() {
          _errorMessage = 'Failed to load reviews. Please try again.';
          _isLoading = false;
        });
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    if (_reviews.isEmpty && !_isLoading && _errorMessage == null) {
      return Container(
        padding: const EdgeInsets.symmetric(vertical: 24),
        child: Center(
          child: Column(
            children: [
              Icon(Icons.rate_review_outlined, size: 48, color: Colors.grey.shade300),
              const SizedBox(height: 12),
              Text(
                'No reviews yet',
                style: TextStyle(
                  fontSize: 16,
                  fontWeight: FontWeight.w500,
                  color: Colors.grey.shade500,
                ),
              ),
              const SizedBox(height: 4),
              Text(
                'Be the first to share your experience!',
                style: TextStyle(
                  fontSize: 13,
                  color: Colors.grey.shade400,
                ),
              ),
            ],
          ),
        ),
      );
    }

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        if (_reviews.isNotEmpty) ...[
          ListView.separated(
            shrinkWrap: true,
            physics: const NeverScrollableScrollPhysics(),
            itemCount: _reviews.length,
            separatorBuilder: (context, index) => Divider(color: Colors.grey.shade200, height: 32),
            itemBuilder: (context, index) {
              final review = _reviews[index];
              return _buildReviewItem(review);
            },
          ),
          const SizedBox(height: 16),
        ],

        if (_isLoading)
          const Padding(
            padding: EdgeInsets.symmetric(vertical: 20),
            child: Center(
              child: CircularProgressIndicator(
                strokeWidth: 2,
                color: Color(0xFF1E3A5F),
              ),
            ),
          ),

        if (_errorMessage != null)
          Padding(
            padding: const EdgeInsets.symmetric(vertical: 10),
            child: Column(
              children: [
                Text(
                  _errorMessage!,
                  style: const TextStyle(color: Colors.red),
                ),
                TextButton(
                  onPressed: _fetchReviews,
                  child: const Text('Retry'),
                ),
              ],
            ),
          ),

        if (_hasMore && !_isLoading && _errorMessage == null)
          Center(
            child: OutlinedButton(
              onPressed: _fetchReviews,
              style: OutlinedButton.styleFrom(
                foregroundColor: const Color(0xFF1E3A5F),
                side: const BorderSide(color: Color(0xFF1E3A5F)),
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(8),
                ),
              ),
              child: const Text('Load More Reviews'),
            ),
          ),
      ],
    );
  }

  Widget _buildReviewItem(Review review) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            Row(
              children: [
                CircleAvatar(
                  radius: 16,
                  backgroundColor: Colors.blue.shade100,
                  child: Text(
                    review.userName.isNotEmpty ? review.userName[0].toUpperCase() : 'U',
                    style: TextStyle(
                      color: Colors.blue.shade800,
                      fontWeight: FontWeight.bold,
                      fontSize: 14,
                    ),
                  ),
                ),
                const SizedBox(width: 10),
                Text(
                  review.userName.isNotEmpty ? review.userName : 'Anonymous',
                  style: const TextStyle(
                    fontWeight: FontWeight.bold,
                    fontSize: 15,
                    color: Color(0xFF111827),
                  ),
                ),
              ],
            ),
            Text(
              DateFormat('MMM dd, yyyy').format(review.createdAt),
              style: TextStyle(
                color: Colors.grey.shade500,
                fontSize: 12,
              ),
            ),
          ],
        ),
        const SizedBox(height: 8),
        Row(
          children: List.generate(5, (index) {
            return Icon(
              index < review.rating ? Icons.star : Icons.star_border,
              color: const Color(0xFFE8A020),
              size: 16,
            );
          }),
        ),
        const SizedBox(height: 8),
        Text(
          review.comment,
          style: TextStyle(
            color: Colors.grey.shade800,
            fontSize: 14,
            height: 1.5,
          ),
        ),
      ],
    );
  }
}
