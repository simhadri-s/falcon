import 'package:flutter/material.dart';
import 'package:webview_flutter/webview_flutter.dart';
import 'package:flutter_application/features/auth/data/auth_service.dart';
import 'package:flutter_application/core/constants/api_constants.dart';

class GoogleLoginPage extends StatefulWidget {
  const GoogleLoginPage({super.key});

  @override
  State<GoogleLoginPage> createState() => _GoogleLoginPageState();
}

class _GoogleLoginPageState extends State<GoogleLoginPage> {
  WebViewController? _controller;
  final AuthService _authService = AuthService();
  bool _isLoading = true;
  String? _errorMessage;

  @override
  void initState() {
    super.initState();
    _initializeWebView();
  }

  void _initializeWebView() {
    try {
      final controller = WebViewController()
        ..setJavaScriptMode(JavaScriptMode.unrestricted)
        ..setNavigationDelegate(
          NavigationDelegate(
            onPageStarted: (url) {
              if (mounted) setState(() => _isLoading = true);
            },
            onPageFinished: (url) {
              if (mounted) setState(() => _isLoading = false);
            },
            onNavigationRequest: (NavigationRequest request) {
              if (request.url.contains('token=')) {
                final uri = Uri.parse(request.url);
                final token = uri.queryParameters['token'];
                if (token != null) {
                  _handleToken(token);
                }
                return NavigationDecision.prevent;
              }
              if (request.url.contains('error=')) {
                final uri = Uri.parse(request.url);
                final error = uri.queryParameters['error'];
                if (error != null) {
                  if (mounted) {
                    setState(() {
                      _errorMessage = error.replaceAll('+', ' ');
                      _isLoading = false;
                    });
                  }
                }
                return NavigationDecision.prevent;
              }
              return NavigationDecision.navigate;
            },
            onWebResourceError: (WebResourceError error) {
              if (mounted) {
                setState(() {
                  _errorMessage = "WebView Error: ${error.description}";
                  _isLoading = false;
                });
              }
            },
          ),
        )
        ..loadRequest(
          Uri.parse('${ApiConstants.hostUrl}/oauth2/authorization/google'),
        );

      setState(() {
        _controller = controller;
      });
    } catch (e) {
      setState(() {
        _errorMessage =
            "Could not initialize WebView. If you just added the package, please perform a COLD RESTART (stop and start the app again).\n\nError: $e";
        _isLoading = false;
      });
    }
  }

  Future<void> _handleToken(String token) async {
    await _authService.saveToken(token);
    if (!mounted) return;
    Navigator.pop(context, true);
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Google Sign In'),
        backgroundColor: Colors.white,
        foregroundColor: Colors.black,
        elevation: 0,
      ),
      body: _buildBody(),
    );
  }

  Widget _buildBody() {
    if (_errorMessage != null) {
      return Center(
        child: Padding(
          padding: const EdgeInsets.all(24.0),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              const Icon(Icons.error_outline, size: 60, color: Colors.red),
              const SizedBox(height: 16),
              Text(
                _errorMessage!,
                textAlign: TextAlign.center,
                style: const TextStyle(color: Colors.red),
              ),
              const SizedBox(height: 24),
              ElevatedButton(
                onPressed: _initializeWebView,
                child: const Text("Retry"),
              ),
            ],
          ),
        ),
      );
    }

    if (_controller == null) {
      return const Center(child: CircularProgressIndicator());
    }

    return Stack(
      children: [
        WebViewWidget(controller: _controller!),
        if (_isLoading)
          const Center(
            child: CircularProgressIndicator(color: Colors.deepPurple),
          ),
      ],
    );
  }
}
