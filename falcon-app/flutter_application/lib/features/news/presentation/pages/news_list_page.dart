import 'package:flutter/material.dart';
import 'package:flutter_application/core/utils/error_handler.dart';
import '../../../home/data/home_api_service.dart';
import '../../../home/data/news_model.dart';
import '../../../home/presentation/widgets/news_card.dart';

class NewsListPage extends StatefulWidget {
  const NewsListPage({super.key});

  @override
  State<NewsListPage> createState() => _NewsListPageState();
}

class _NewsListPageState extends State<NewsListPage> {
  final HomeApiService _apiService = HomeApiService();
  final ScrollController _scrollController = ScrollController();

  List<News> _news = [];
  bool _isLoading = false;
  bool _hasMore = true;
  int _currentPage = 1;
  final int _pageSize = 10;
  String? _errorMessage;

  @override
  void initState() {
    super.initState();
    _fetchNews();
    _scrollController.addListener(_onScroll);
  }

  @override
  void dispose() {
    _scrollController.dispose();
    super.dispose();
  }

  void _onScroll() {
    if (_scrollController.position.pixels >=
            _scrollController.position.maxScrollExtent - 200 &&
        !_isLoading &&
        _hasMore) {
      _fetchNews();
    }
  }

  Future<void> _fetchNews({bool reset = false}) async {
    if (reset) {
      setState(() {
        _currentPage = 1;
        _news = [];
        _hasMore = true;
      });
    }

    if (_isLoading || !_hasMore) return;

    setState(() {
      _isLoading = true;
    });

    try {
      final response = await _apiService.getNews(
        page: _currentPage,
        size: _pageSize,
      );

      setState(() {
        _isLoading = false;
        _errorMessage = null;
        _news.addAll(response.news);
        if (response.news.length < _pageSize ||
            _news.length >= response.total) {
          _hasMore = false;
        }
        _currentPage++;
      });
    } catch (e) {
      setState(() {
        _isLoading = false;
        if (_news.isEmpty) {
          _errorMessage = ErrorHandler.messageFor(e);
        }
      });
      if (mounted) {
        ErrorHandler.showError(context, e);
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text("News Section"),
        backgroundColor: Colors.white,
        foregroundColor: Colors.black,
        elevation: 0,
      ),
      backgroundColor: Colors.grey.shade50,
      body: RefreshIndicator(
        onRefresh: () => _fetchNews(reset: true),
        child: _news.isEmpty && _isLoading
            ? const Center(child: CircularProgressIndicator())
            : _errorMessage != null && _news.isEmpty
            ? _buildErrorState(_errorMessage!)
            : _news.isEmpty
            ? const Center(child: Text("No news articles available"))
            : ListView.builder(
                controller: _scrollController,
                physics: const AlwaysScrollableScrollPhysics(),
                padding: const EdgeInsets.all(16),
                itemCount: _news.length + (_hasMore ? 1 : 0),
                itemBuilder: (context, index) {
                  if (index == _news.length) {
                    return const Center(
                      child: Padding(
                        padding: EdgeInsets.symmetric(vertical: 16),
                        child: CircularProgressIndicator(),
                      ),
                    );
                  }
                  return Padding(
                    padding: const EdgeInsets.only(bottom: 16),
                    child: NewsCard(news: _news[index]),
                  );
                },
              ),
      ),
    );
  }

  Widget _buildErrorState(String message) {
    return ListView(
      physics: const AlwaysScrollableScrollPhysics(),
      children: [
        SizedBox(
          height: 420,
          child: Center(
            child: Padding(
              padding: const EdgeInsets.all(32),
              child: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  Icon(
                    Icons.error_outline,
                    size: 64,
                    color: Colors.red.shade300,
                  ),
                  const SizedBox(height: 16),
                  const Text(
                    "Unable to load news",
                    style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
                  ),
                  const SizedBox(height: 8),
                  Text(
                    message,
                    textAlign: TextAlign.center,
                    style: TextStyle(color: Colors.grey.shade600),
                  ),
                  const SizedBox(height: 20),
                  ElevatedButton(
                    onPressed: () => _fetchNews(reset: true),
                    child: const Text("Retry"),
                  ),
                ],
              ),
            ),
          ),
        ),
      ],
    );
  }
}
