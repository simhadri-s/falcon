import 'package:flutter/material.dart';
import 'package:flutter_application/core/widgets/retry_image.dart';
import 'package:flutter_application/features/home/data/news_model.dart';
import 'package:provider/provider.dart';
import 'package:share_plus/share_plus.dart';
import '../../../company/presentation/providers/company_provider.dart';

class NewsDetailPage extends StatelessWidget {
  final News news;

  const NewsDetailPage({super.key, required this.news});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.white,
      body: CustomScrollView(
        slivers: [
          // Header with back button and category
          SliverAppBar(
            pinned: true,
            backgroundColor: Colors.white,
            elevation: 0,
            leading: IconButton(
              icon: const Icon(Icons.arrow_back_ios_new, color: Colors.black87),
              onPressed: () => Navigator.pop(context),
            ),
            actions: [
              IconButton(
                icon: const Icon(Icons.share_outlined, color: Colors.black87),
                onPressed: () {
                  Share.share('${news.title}\n\n${news.content}');
                },
              ),
            ],
            title: Text(
              news.category.toUpperCase(),
              style: TextStyle(
                color: Colors.deepPurple.shade300,
                fontSize: 12,
                fontWeight: FontWeight.bold,
                letterSpacing: 1.2,
              ),
            ),
            centerTitle: true,
          ),

          // News Content
          SliverToBoxAdapter(
            child: Padding(
              padding: const EdgeInsets.symmetric(horizontal: 24),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const SizedBox(height: 16),
                  
                  // Title
                  Text(
                    news.title,
                    style: const TextStyle(
                      fontSize: 32,
                      fontWeight: FontWeight.bold,
                      color: Colors.black87,
                      height: 1.2,
                      letterSpacing: -0.5,
                    ),
                  ),
                  
                  const SizedBox(height: 24),
                  
                  // Author & Date (Mock)
                  Row(
                    children: [
                      const CircleAvatar(
                        radius: 18,
                        backgroundColor: Colors.deepPurple,
                        child: Icon(Icons.person, color: Colors.white, size: 20),
                      ),
                      const SizedBox(width: 12),
                      Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Consumer<CompanyProvider>(
                            builder: (context, company, child) {
                              return Text(
                                "${company.companyName} Editor",
                                style: const TextStyle(
                                  fontWeight: FontWeight.bold,
                                  fontSize: 14,
                                ),
                              );
                            },
                          ),
                          Text(
                            "April 27, 2026 • 5 min read",
                            style: TextStyle(
                              color: Colors.grey.shade600,
                              fontSize: 12,
                            ),
                          ),
                        ],
                      ),
                    ],
                  ),
                  
                  const SizedBox(height: 32),

                  // Image(s)
                  if (news.imageUrls.isNotEmpty)
                    ...news.imageUrls.map((url) => Padding(
                      padding: const EdgeInsets.only(bottom: 16),
                      child: ClipRRect(
                        borderRadius: BorderRadius.circular(16),
                        child: RetryNetworkImage(
                          imageUrl: url,
                          width: double.infinity,
                          height: 240,
                          fit: BoxFit.cover,
                        ),
                      ),
                    ))
                  else
                    ClipRRect(
                      borderRadius: BorderRadius.circular(16),
                      child: RetryNetworkImage(
                        imageUrl: 'https://via.placeholder.com/600x300',
                        width: double.infinity,
                        height: 240,
                        fit: BoxFit.cover,
                      ),
                    ),

                  const SizedBox(height: 24),

                  // Content
                  Text(
                    news.content,
                    style: TextStyle(
                      fontSize: 18,
                      color: Colors.grey.shade800,
                      height: 1.7,
                      letterSpacing: 0.2,
                    ),
                  ),
                  
                  const SizedBox(height: 48),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }
}
