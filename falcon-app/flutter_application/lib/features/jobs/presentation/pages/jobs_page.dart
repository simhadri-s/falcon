import 'package:flutter/material.dart';
import '../../data/jobs_api_service.dart';
import '../../data/job_model.dart';
import 'job_application_page.dart';
import 'package:flutter_application/features/home/presentation/widgets/header.dart';
import 'package:flutter_application/core/widgets/app_drawer.dart';
import 'package:flutter_application/core/utils/auth_guard.dart';
import 'package:flutter_application/core/utils/error_handler.dart';

class JobsPage extends StatefulWidget {
  const JobsPage({super.key});

  @override
  State<JobsPage> createState() => _JobsPageState();
}

class _JobsPageState extends State<JobsPage> {
  final JobsApiService _apiService = JobsApiService();
  final ScrollController _scrollController = ScrollController();

  List<Job> _jobs = [];
  bool _isLoading = false;
  bool _hasMore = true;
  int _currentPage = 0;
  final int _pageSize = 10;
  String? _errorMessage;

  @override
  void initState() {
    super.initState();
    _fetchJobs();
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
      _fetchJobs();
    }
  }

  Future<void> _fetchJobs({bool reset = false}) async {
    if (reset) {
      setState(() {
        _currentPage = 0;
        _jobs = [];
        _hasMore = true;
      });
    }

    if (_isLoading || !_hasMore) return;

    setState(() {
      _isLoading = true;
    });

    try {
      final response = await _apiService.getJobs(
        page: _currentPage,
        size: _pageSize,
      );

      setState(() {
        _isLoading = false;
        _errorMessage = null;
        _jobs.addAll(response.jobs);
        if (response.jobs.length < _pageSize ||
            _jobs.length >= response.total) {
          _hasMore = false;
        }
        _currentPage++;
      });
    } catch (e) {
      setState(() {
        _isLoading = false;
        if (_jobs.isEmpty) {
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
      drawer: const AppDrawer(),
      backgroundColor: Colors.white,
      body: SafeArea(
        child: Column(
          children: [
            const Header(),
            Expanded(
              child: RefreshIndicator(
                onRefresh: () => _fetchJobs(reset: true),
                child: _jobs.isEmpty && _isLoading
                    ? const Center(
                        child: CircularProgressIndicator(
                          color: Colors.deepPurple,
                        ),
                      )
                    : _errorMessage != null && _jobs.isEmpty
                    ? _buildErrorWidget(_errorMessage!)
                    : _jobs.isEmpty
                    ? _buildEmptyWidget()
                    : ListView.builder(
                        controller: _scrollController,
                        physics: const AlwaysScrollableScrollPhysics(),
                        padding: const EdgeInsets.all(20),
                        itemCount: _jobs.length + (_hasMore ? 1 : 0),
                        itemBuilder: (context, index) {
                          if (index == _jobs.length) {
                            return const Center(
                              child: Padding(
                                padding: EdgeInsets.symmetric(vertical: 16),
                                child: CircularProgressIndicator(
                                  color: Colors.deepPurple,
                                ),
                              ),
                            );
                          }
                          final job = _jobs[index];
                          return _buildJobCard(context, job);
                        },
                      ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildErrorWidget(String error) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(40),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(Icons.error_outline, size: 64, color: Colors.red.shade300),
            const SizedBox(height: 16),
            const Text(
              "Something went wrong",
              style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
            ),
            const SizedBox(height: 8),
            Text(
              error,
              textAlign: TextAlign.center,
              style: TextStyle(color: Colors.grey.shade600),
            ),
            const SizedBox(height: 24),
            ElevatedButton(
              onPressed: () => _fetchJobs(reset: true),
              style: ElevatedButton.styleFrom(
                backgroundColor: Colors.deepPurple,
              ),
              child: const Text("Retry", style: TextStyle(color: Colors.white)),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildEmptyWidget() {
    return Center(
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Icon(Icons.work_outline, size: 64, color: Colors.grey.shade300),
          const SizedBox(height: 16),
          const Text(
            "No Openings Currently",
            style: TextStyle(
              fontSize: 18,
              fontWeight: FontWeight.bold,
              color: Colors.grey,
            ),
          ),
          const SizedBox(height: 8),
          const Text("Check back later for new opportunities"),
        ],
      ),
    );
  }

  Widget _buildJobCard(BuildContext context, Job job) {
    return Container(
      margin: const EdgeInsets.only(bottom: 20),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(20),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withValues(alpha: 0.05),
            blurRadius: 20,
            offset: const Offset(0, 10),
          ),
        ],
      ),
      child: ClipRRect(
        borderRadius: BorderRadius.circular(20),
        child: Material(
          color: Colors.transparent,
          child: InkWell(
            onTap: () async {
              final loggedIn = await AuthGuard.checkLoginOrRedirect(
                context,
                message: 'Please login to apply for jobs',
              );
              if (!loggedIn) return;
              if (context.mounted) {
                Navigator.push(
                  context,
                  MaterialPageRoute(
                    builder: (context) => JobApplicationPage(job: job),
                  ),
                );
              }
            },
            child: Padding(
              padding: const EdgeInsets.all(20),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      Expanded(
                        child: Text(
                          job.title,
                          style: const TextStyle(
                            fontSize: 20,
                            fontWeight: FontWeight.bold,
                            letterSpacing: -0.5,
                          ),
                        ),
                      ),
                      Container(
                        padding: const EdgeInsets.symmetric(
                          horizontal: 10,
                          vertical: 6,
                        ),
                        decoration: BoxDecoration(
                          color: Colors.deepPurple.shade50,
                          borderRadius: BorderRadius.circular(10),
                        ),
                        child: Text(
                          job.type,
                          style: TextStyle(
                            color: Colors.deepPurple.shade700,
                            fontSize: 12,
                            fontWeight: FontWeight.bold,
                          ),
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 12),
                  Row(
                    children: [
                      _buildInfoTag(Icons.location_on_outlined, job.location),
                      const SizedBox(width: 16),
                      _buildInfoTag(Icons.business_outlined, job.department),
                    ],
                  ),
                  const SizedBox(height: 16),
                  Text(
                    job.description,
                    maxLines: 3,
                    overflow: TextOverflow.ellipsis,
                    style: TextStyle(
                      color: Colors.grey.shade600,
                      fontSize: 14,
                      height: 1.5,
                    ),
                  ),
                  const SizedBox(height: 20),
                  Row(
                    children: [
                      const Text(
                        "View Details & Apply",
                        style: TextStyle(
                          color: Colors.deepPurple,
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                      const SizedBox(width: 4),
                      Icon(
                        Icons.arrow_forward,
                        size: 16,
                        color: Colors.deepPurple.shade700,
                      ),
                    ],
                  ),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildInfoTag(IconData icon, String label) {
    return Row(
      mainAxisSize: MainAxisSize.min,
      children: [
        Icon(icon, size: 16, color: Colors.grey.shade500),
        const SizedBox(width: 4),
        Text(
          label,
          style: TextStyle(color: Colors.grey.shade600, fontSize: 13),
        ),
      ],
    );
  }
}
