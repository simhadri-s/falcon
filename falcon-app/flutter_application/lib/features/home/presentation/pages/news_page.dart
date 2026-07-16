import 'package:flutter/material.dart';
import '../widgets/news_card.dart';
import '../../data/home_api_service.dart';

class NewsPage extends StatefulWidget {
  const NewsPage({super.key});

  @override
  State<NewsPage> createState() => _NewsPageState();
}

class _NewsPageState extends State<NewsPage> {
  final HomeApiService _apiService = HomeApiService();
  late Future<PaginatedNews> _newsFuture;

  @override
  void initState() {
    super.initState();
    _newsFuture = _apiService.getNews(size: 20);
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.grey.shade50,
      appBar: AppBar(
        title: const Text(
          "Latest News",
          style: TextStyle(fontWeight: FontWeight.bold, color: Colors.black87),
        ),
        backgroundColor: Colors.white,
        elevation: 0,
        leading: IconButton(
          icon: const Icon(Icons.arrow_back_ios_new, color: Colors.black87),
          onPressed: () => Navigator.pop(context),
        ),
      ),
      body: FutureBuilder<PaginatedNews>(
        future: _newsFuture,
        builder: (context, snapshot) {
          if (snapshot.connectionState == ConnectionState.waiting) {
            return const Center(child: CircularProgressIndicator());
          } else if (snapshot.hasError) {
            return Center(child: Text("Error loading news: ${snapshot.error}"));
          } else if (!snapshot.hasData || snapshot.data!.news.isEmpty) {
            return const Center(child: Text("No news available"));
          }

          final newsList = snapshot.data!.news;

          return ListView.builder(
            padding: const EdgeInsets.all(16),
            itemCount: newsList.length,
            itemBuilder: (context, index) {
              return Padding(
                padding: const EdgeInsets.only(bottom: 16),
                child: NewsCard(
                  news: newsList[index],
                  isHorizontal: false, // Use vertical layout for listing page
                ),
              );
            },
          );
        },
      ),
    );
  }
}
