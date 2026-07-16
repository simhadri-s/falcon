import 'package:flutter/material.dart';
import 'package:flutter_application/features/home/data/product_model.dart';

class ReviewSummaryWidget extends StatelessWidget {
  final Product product;

  const ReviewSummaryWidget({super.key, required this.product});

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: Colors.grey.shade200),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withOpacity(0.02),
            blurRadius: 4,
            offset: const Offset(0, 2),
          ),
        ],
      ),
      child: Row(
        children: [
          Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                product.reviewCount > 0 
                  ? product.averageRating.toStringAsFixed(1)
                  : 'No Ratings',
                style: TextStyle(
                  fontSize: product.reviewCount > 0 ? 42 : 24,
                  fontWeight: FontWeight.w900,
                  color: const Color(0xFF111827),
                  letterSpacing: -1,
                ),
              ),
              const SizedBox(height: 4),
              Row(
                children: List.generate(5, (index) {
                  final isFull = product.reviewCount > 0 && index < product.averageRating.floor();
                  final isHalf = product.reviewCount > 0 &&
                      index == product.averageRating.floor() &&
                      (product.averageRating - product.averageRating.floor()) >=
                          0.5;

                  return Icon(
                    isFull
                        ? Icons.star
                        : (isHalf ? Icons.star_half : Icons.star_border),
                    color: const Color(0xFFE8A020),
                    size: 20,
                  );
                }),
              ),
              if (product.reviewCount > 0) ...[
                const SizedBox(height: 6),
                Text(
                  'Based on ${product.reviewCount} ${product.reviewCount == 1 ? 'review' : 'reviews'}',
                  style: TextStyle(
                    fontSize: 13,
                    color: Colors.grey.shade600,
                    fontWeight: FontWeight.w500,
                  ),
                ),
              ],
            ],
          ),
          const SizedBox(width: 24),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text(
                  'Customer Feedback',
                  style: TextStyle(
                    fontSize: 16,
                    fontWeight: FontWeight.bold,
                    color: Colors.black87,
                  ),
                ),
                const SizedBox(height: 8),
                Text(
                  'Ratings and reviews are provided by verified customers who have purchased this item.',
                  style: TextStyle(
                    fontSize: 12,
                    color: Colors.grey.shade600,
                    height: 1.4,
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
