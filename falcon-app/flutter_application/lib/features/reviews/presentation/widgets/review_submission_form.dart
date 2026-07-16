import 'package:flutter/material.dart';
import 'package:flutter_application/features/reviews/data/review_model.dart';
import 'package:flutter_application/features/reviews/data/reviews_api_service.dart';
import 'package:flutter_application/core/utils/auth_guard.dart';
import 'package:flutter_application/features/auth/data/auth_service.dart';

class ReviewSubmissionForm extends StatefulWidget {
  final String productId;
  final VoidCallback onReviewSubmitted;

  const ReviewSubmissionForm({
    super.key,
    required this.productId,
    required this.onReviewSubmitted,
  });

  @override
  State<ReviewSubmissionForm> createState() => _ReviewSubmissionFormState();
}

class _ReviewSubmissionFormState extends State<ReviewSubmissionForm> {
  final _formKey = GlobalKey<FormState>();
  final _commentController = TextEditingController();
  int _rating = 0; // Start with 0 stars (no selection)
  bool _isSubmitting = false;
  bool _isLoading = true;
  bool _isEditMode = false;
  bool _isEligible = false;
  bool _isLoggedIn = false;
  Review? _existingReview;

  final ReviewsApiService _apiService = ReviewsApiService();
  final AuthService _authService = AuthService();

  @override
  void initState() {
    super.initState();
    _checkStatus();
  }

  @override
  void dispose() {
    _commentController.dispose();
    super.dispose();
  }

  Future<void> _checkStatus() async {
    try {
      final token = await _authService.getToken();
      _isLoggedIn = token != null;

      if (_isLoggedIn) {
        // Check if user already reviewed
        final review = await _apiService.getUserReview(widget.productId);
        if (review != null) {
          if (mounted) {
            setState(() {
              _existingReview = review;
              _isEditMode = true;
              _isEligible = true; // If they reviewed, they were eligible
              _rating = review.rating;
              _commentController.text = review.comment;
              _isLoading = false;
            });
          }
          return;
        }

        // If not reviewed, check purchase eligibility
        final eligible = await _apiService.checkEligibility(widget.productId);
        if (mounted) {
          setState(() {
            _isEligible = eligible;
            _isLoading = false;
          });
        }
      } else {
        if (mounted) {
          setState(() {
            _isEligible = false;
            _isLoading = false;
          });
        }
      }
    } catch (e) {
      if (mounted) {
        setState(() {
          _isLoading = false;
        });
      }
    }
  }

  Future<void> _submitReview() async {
    final loggedIn = await AuthGuard.checkLoginOrRedirect(
      context,
      message: 'Please login to submit a review',
    );
    if (!loggedIn) return;

    if (_rating == 0) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Please select a rating'),
          backgroundColor: Colors.orange,
        ),
      );
      return;
    }

    if (!_formKey.currentState!.validate()) return;

    setState(() {
      _isSubmitting = true;
    });

    try {
      final ReviewResponse response;
      
      if (_isEditMode) {
        response = await _apiService.updateReview(
          productId: widget.productId,
          rating: _rating,
          comment: _commentController.text.trim(),
        );
      } else {
        response = await _apiService.addReview(
          productId: widget.productId,
          rating: _rating,
          comment: _commentController.text.trim(),
        );
      }

      if (!mounted) return;

      if (response.success) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(_isEditMode ? 'Review updated successfully!' : 'Review submitted successfully!'),
            backgroundColor: Colors.green,
          ),
        );
        if (!_isEditMode) {
          // After first submission, switch to edit mode
          setState(() {
            _isEditMode = true;
            _existingReview = response.review;
          });
        }
        widget.onReviewSubmitted();
      } else {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(response.message),
            backgroundColor: Colors.red,
          ),
        );
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('An error occurred. Please try again.'),
            backgroundColor: Colors.red,
          ),
        );
      }
    } finally {
      if (mounted) {
        setState(() {
          _isSubmitting = false;
        });
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    if (_isLoading) {
      return Container(
        padding: const EdgeInsets.all(20),
        decoration: BoxDecoration(
          color: Colors.white,
          borderRadius: BorderRadius.circular(12),
          border: Border.all(color: Colors.grey.shade200),
        ),
        child: const Center(
          child: SizedBox(
            height: 24,
            width: 24,
            child: CircularProgressIndicator(strokeWidth: 2, color: Color(0xFF1E3A5F)),
          ),
        ),
      );
    }

    if (!_isLoggedIn) {
      return _buildMessageContainer(
        'Want to review this product?',
        'Please login to share your experience with other customers.',
        buttonText: 'Login to Review',
        onButtonPressed: () => AuthGuard.checkLoginOrRedirect(context),
      );
    }

    if (!_isEligible && !_isEditMode) {
      return _buildMessageContainer(
        'Verified Purchase Required',
        'Only customers who have purchased this product and received it can leave a review.',
        icon: Icons.verified_user_outlined,
      );
    }

    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: Colors.grey.shade200),
      ),
      child: Form(
        key: _formKey,
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Text(
                  _isEditMode ? 'Edit Your Review' : 'Write a Review',
                  style: const TextStyle(
                    fontSize: 18,
                    fontWeight: FontWeight.bold,
                    color: Color(0xFF111827),
                  ),
                ),
                if (_isEditMode) ...[
                  const SizedBox(width: 8),
                  Container(
                    padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
                    decoration: BoxDecoration(
                      color: Colors.blue.shade50,
                      borderRadius: BorderRadius.circular(12),
                    ),
                    child: Text(
                      'Editing',
                      style: TextStyle(
                        fontSize: 11,
                        fontWeight: FontWeight.w600,
                        color: Colors.blue.shade700,
                      ),
                    ),
                  ),
                ],
              ],
            ),
            const SizedBox(height: 16),
            const Text(
              'Rating',
              style: TextStyle(
                fontSize: 14,
                fontWeight: FontWeight.w600,
                color: Colors.black87,
              ),
            ),
            const SizedBox(height: 8),
            Row(
              children: List.generate(5, (index) {
                return IconButton(
                  padding: EdgeInsets.zero,
                  constraints: const BoxConstraints(),
                  icon: Icon(
                    index < _rating ? Icons.star : Icons.star_border,
                    color: const Color(0xFFE8A020),
                    size: 32,
                  ),
                  onPressed: () {
                    setState(() {
                      _rating = index + 1;
                    });
                  },
                );
              }),
            ),
            if (_rating == 0)
              Padding(
                padding: const EdgeInsets.only(top: 4),
                child: Text(
                  'Tap a star to rate',
                  style: TextStyle(
                    fontSize: 12,
                    color: Colors.grey.shade500,
                    fontStyle: FontStyle.italic,
                  ),
                ),
              ),
            const SizedBox(height: 20),
            const Text(
              'Your Review',
              style: TextStyle(
                fontSize: 14,
                fontWeight: FontWeight.w600,
                color: Colors.black87,
              ),
            ),
            const SizedBox(height: 8),
            TextFormField(
              controller: _commentController,
              maxLines: 4,
              decoration: InputDecoration(
                hintText: 'What did you like or dislike? What did you use this product for?',
                hintStyle: TextStyle(color: Colors.grey.shade400, fontSize: 14),
                border: OutlineInputBorder(
                  borderRadius: BorderRadius.circular(8),
                  borderSide: BorderSide(color: Colors.grey.shade300),
                ),
                enabledBorder: OutlineInputBorder(
                  borderRadius: BorderRadius.circular(8),
                  borderSide: BorderSide(color: Colors.grey.shade300),
                ),
                focusedBorder: OutlineInputBorder(
                  borderRadius: BorderRadius.circular(8),
                  borderSide: BorderSide(color: Colors.blue.shade300),
                ),
                filled: true,
                fillColor: Colors.grey.shade50,
              ),
              validator: (value) {
                if (value == null || value.trim().isEmpty) {
                  return 'Please enter a review';
                }
                return null;
              },
            ),
            const SizedBox(height: 20),
            SizedBox(
              width: double.infinity,
              height: 48,
              child: ElevatedButton(
                onPressed: _isSubmitting ? null : _submitReview,
                style: ElevatedButton.styleFrom(
                  backgroundColor: const Color(0xFF111827),
                  foregroundColor: Colors.white,
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(8),
                  ),
                  elevation: 0,
                ),
                child: _isSubmitting
                    ? const SizedBox(
                        height: 20,
                        width: 20,
                        child: CircularProgressIndicator(
                          strokeWidth: 2,
                          color: Colors.white,
                        ),
                      )
                    : Text(
                        _isEditMode ? 'Update Review' : 'Submit Review',
                        style: const TextStyle(
                          fontSize: 16,
                          fontWeight: FontWeight.bold,
                        ),
                      ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildMessageContainer(String title, String message, {IconData icon = Icons.info_outline, String? buttonText, VoidCallback? onButtonPressed}) {
    return Container(
      padding: const EdgeInsets.all(24),
      decoration: BoxDecoration(
        color: Colors.grey.shade50,
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: Colors.grey.shade200),
      ),
      child: Column(
        children: [
          Icon(icon, size: 40, color: Colors.grey.shade400),
          const SizedBox(height: 16),
          Text(
            title,
            style: const TextStyle(
              fontSize: 16,
              fontWeight: FontWeight.bold,
              color: Color(0xFF111827),
            ),
            textAlign: TextAlign.center,
          ),
          const SizedBox(height: 8),
          Text(
            message,
            style: TextStyle(
              fontSize: 14,
              color: Colors.grey.shade600,
            ),
            textAlign: TextAlign.center,
          ),
          if (buttonText != null) ...[
            const SizedBox(height: 20),
            SizedBox(
              width: double.infinity,
              height: 44,
              child: OutlinedButton(
                onPressed: onButtonPressed,
                style: OutlinedButton.styleFrom(
                  foregroundColor: const Color(0xFF111827),
                  side: const BorderSide(color: Color(0xFF111827)),
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(8),
                  ),
                ),
                child: Text(buttonText),
              ),
            ),
          ],
        ],
      ),
    );
  }
}
