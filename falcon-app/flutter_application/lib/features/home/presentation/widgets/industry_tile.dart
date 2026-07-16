import 'package:flutter/material.dart';
import 'package:flutter_application/features/home/data/industry_model.dart';
import 'package:flutter_application/core/widgets/retry_image.dart';

// Color palette for industry gradient backgrounds
const List<List<Color>> _industryGradients = [
  [Color(0xFF6366F1), Color(0xFF8B5CF6)],
  [Color(0xFFF97316), Color(0xFFFB923C)],
  [Color(0xFF10B981), Color(0xFF34D399)],
  [Color(0xFFEF4444), Color(0xFFF87171)],
  [Color(0xFF3B82F6), Color(0xFF60A5FA)],
  [Color(0xFFF59E0B), Color(0xFFFBBF24)],
  [Color(0xFF8B5CF6), Color(0xFFA78BFA)],
  [Color(0xFF06B6D4), Color(0xFF22D3EE)],
];

class IndustryTile extends StatelessWidget {
  final Industry industry;
  final VoidCallback onTap;
  final int index;

  const IndustryTile({
    super.key,
    required this.industry,
    required this.onTap,
    this.index = 0,
  });

  @override
  Widget build(BuildContext context) {
    final gradient =
        _industryGradients[index % _industryGradients.length];

    return GestureDetector(
      onTap: onTap,
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          // Circle icon
          Container(
            width: 60,
            height: 60,
            decoration: BoxDecoration(
              gradient: LinearGradient(
                colors: gradient,
                begin: Alignment.topLeft,
                end: Alignment.bottomRight,
              ),
              shape: BoxShape.circle,
              boxShadow: [
                BoxShadow(
                  color: gradient[0].withValues(alpha: 0.35),
                  blurRadius: 12,
                  offset: const Offset(0, 5),
                ),
              ],
            ),
            child: ClipOval(
              child: industry.iconUrl != null &&
                      industry.iconUrl!.isNotEmpty
                  ? RetryNetworkImage(
                      imageUrl: industry.iconUrl!,
                      fit: BoxFit.cover,
                    )
                  : const Icon(
                      Icons.business_outlined,
                      color: Colors.white,
                      size: 26,
                    ),
            ),
          ),
          const SizedBox(height: 6),
          // Name label
          SizedBox(
            width: 76,
            child: Text(
              industry.name,
              textAlign: TextAlign.center,
              maxLines: 2,
              overflow: TextOverflow.ellipsis,
              style: const TextStyle(
                fontSize: 10,
                fontWeight: FontWeight.w600,
                color: Color(0xFF1A1A2E),
                height: 1.2,
              ),
            ),
          ),
        ],
      ),
    );
  }
}
