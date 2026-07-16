import 'package:flutter/material.dart';
import 'package:cached_network_image/cached_network_image.dart';

class RetryNetworkImage extends StatelessWidget {
  final String imageUrl;
  final double? width;
  final double? height;
  final BoxFit fit;
  final int maxRetries;
  final Widget? placeholder;
  final Widget? errorWidget;

  const RetryNetworkImage({
    super.key,
    required this.imageUrl,
    this.width,
    this.height,
    this.fit = BoxFit.cover,
    this.maxRetries = 3, // Kept for API compatibility, though CachedNetworkImage handles retries internally
    this.placeholder,
    this.errorWidget,
  });

  @override
  Widget build(BuildContext context) {
    if (imageUrl.isEmpty) return _buildError();

    return CachedNetworkImage(
      imageUrl: imageUrl,
      width: width,
      height: height,
      fit: fit,
      placeholder: (context, url) => _buildLoading(),
      errorWidget: (context, url, error) => _buildError(),
      // CachedNetworkImage automatically uses an LRU cache for memory and disk
      fadeInDuration: const Duration(milliseconds: 300),
    );
  }

  Widget _buildLoading() {
    return placeholder ?? Container(
      width: width,
      height: height,
      color: Colors.grey.shade100,
      child: const Center(
        child: SizedBox(
          width: 20,
          height: 20,
          child: CircularProgressIndicator(strokeWidth: 2, color: Colors.deepPurple),
        ),
      ),
    );
  }

  Widget _buildError() {
    return errorWidget ?? Container(
      width: width,
      height: height,
      color: Colors.grey.shade100,
      child: const Icon(Icons.image_not_supported_outlined, color: Colors.grey),
    );
  }
}
