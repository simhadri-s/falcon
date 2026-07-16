import 'dart:ui';
import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../../../core/navigation/main_nav.dart';
import '../../../../core/widgets/retry_image.dart';
import '../../../company/presentation/providers/company_provider.dart';

class SplashScreen extends StatefulWidget {
  const SplashScreen({super.key});

  @override
  State<SplashScreen> createState() => _SplashScreenState();
}

class _SplashScreenState extends State<SplashScreen> {
  bool _visible = false;

  @override
  void initState() {
    super.initState();
    _startInitialization();
  }

  Future<void> _startInitialization() async {
    // Soft fade-in animation trigger
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (mounted) {
        setState(() {
          _visible = true;
        });
      }
    });

    final startTime = DateTime.now();

    // Fetch and wait for latest company information from API
    final companyProvider = Provider.of<CompanyProvider>(context, listen: false);
    if (companyProvider.apiFetchFuture != null) {
      try {
        await companyProvider.apiFetchFuture!.timeout(
          const Duration(seconds: 3),
          onTimeout: () {
            debugPrint('SplashScreen: Company API fetch timed out');
          },
        );
      } catch (e) {
        debugPrint('SplashScreen: Error fetching company info: $e');
      }
    }

    // Ensure the user gets to appreciate the premium brand experience for at least 2.5 seconds
    final elapsed = DateTime.now().difference(startTime);
    final remaining = const Duration(milliseconds: 2500) - elapsed;
    if (remaining > Duration.zero) {
      await Future.delayed(remaining);
    }

    // Smooth transition to main app navigation
    if (mounted) {
      Navigator.pushReplacement(
        context,
        PageRouteBuilder(
          pageBuilder: (context, animation, secondaryAnimation) => const MainNav(),
          transitionsBuilder: (context, animation, secondaryAnimation, child) {
            return FadeTransition(
              opacity: animation,
              child: child,
            );
          },
          transitionDuration: const Duration(milliseconds: 600),
        ),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    final company = context.watch<CompanyProvider>();
    final hasLandingImage = company.landingPageImageUrl.isNotEmpty;

    return Scaffold(
      backgroundColor: const Color(0xFF131127), // Sleek deep dark mode background
      body: Stack(
        fit: StackFit.expand,
        children: [
          // ── Background image or fallback gradient ─────────────────
          if (hasLandingImage)
            Opacity(
              opacity: 0.85,
              child: RetryNetworkImage(
                imageUrl: company.landingPageImageUrl,
                fit: BoxFit.cover,
              ),
            )
          else
            Container(
              decoration: const BoxDecoration(
                gradient: LinearGradient(
                  colors: [
                    Color(0xFF121026),
                    Color(0xFF1E1B4B),
                    Color(0xFF2E1065),
                  ],
                  begin: Alignment.topLeft,
                  end: Alignment.bottomRight,
                ),
              ),
            ),

          // ── Soft backdrop blur & premium dark overlay ─────────────
          if (hasLandingImage)
            BackdropFilter(
              filter: ImageFilter.blur(sigmaX: 8, sigmaY: 8),
              child: Container(
                color: const Color(0xFF131127).withValues(alpha: 0.45),
              ),
            ),
          
          // Secondary dark gradient vignette overlay for excellent legibility
          Container(
            decoration: BoxDecoration(
              gradient: LinearGradient(
                colors: [
                  Colors.transparent,
                  const Color(0xFF131127).withValues(alpha: 0.3),
                  const Color(0xFF131127).withValues(alpha: 0.85),
                  const Color(0xFF131127),
                ],
                begin: Alignment.topCenter,
                end: Alignment.bottomCenter,
              ),
            ),
          ),

          // ── Centered Brand Content ────────────────────────────────
          SafeArea(
            child: AnimatedOpacity(
              duration: const Duration(milliseconds: 1000),
              curve: Curves.easeInOut,
              opacity: _visible ? 1.0 : 0.0,
              child: Padding(
                padding: const EdgeInsets.symmetric(horizontal: 32.0),
                child: Column(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    const Spacer(flex: 3),

                    // Logo circle with outer glow & shadow
                    Container(
                      width: 110,
                      height: 110,
                      decoration: BoxDecoration(
                        color: Colors.white,
                        shape: BoxShape.circle,
                        boxShadow: [
                          BoxShadow(
                            color: const Color(0xFF4F46E5).withValues(alpha: 0.4),
                            blurRadius: 32,
                            spreadRadius: 4,
                          ),
                          BoxShadow(
                            color: Colors.black.withValues(alpha: 0.15),
                            blurRadius: 16,
                            offset: const Offset(0, 8),
                          ),
                        ],
                      ),
                      padding: const EdgeInsets.all(4),
                      child: ClipOval(
                        child: company.logoUrl.isNotEmpty
                            ? RetryNetworkImage(
                                imageUrl: company.logoUrl,
                                fit: BoxFit.contain,
                              )
                            : Container(
                                color: const Color(0xFF4F46E5),
                                child: const Icon(
                                  Icons.store_outlined,
                                  color: Colors.white,
                                  size: 48,
                                ),
                              ),
                      ),
                    ),
                    const SizedBox(height: 36),

                    // Company Name
                    Text(
                      company.companyName,
                      textAlign: TextAlign.center,
                      style: const TextStyle(
                        fontSize: 32,
                        fontWeight: FontWeight.w900,
                        color: Colors.white,
                        letterSpacing: -0.5,
                        height: 1.2,
                        shadows: [
                          Shadow(
                            color: Colors.black38,
                            blurRadius: 8,
                            offset: Offset(0, 3),
                          ),
                        ],
                      ),
                    ),
                    const SizedBox(height: 12),

                    // Company Tagline / Description
                    if (company.companyDescription.isNotEmpty)
                      Text(
                        company.companyDescription,
                        textAlign: TextAlign.center,
                        style: TextStyle(
                          fontSize: 14,
                          fontWeight: FontWeight.w400,
                          color: Colors.white.withValues(alpha: 0.75),
                          letterSpacing: 0.2,
                          height: 1.5,
                        ),
                        maxLines: 3,
                        overflow: TextOverflow.ellipsis,
                      ),

                    const Spacer(flex: 2),

                    // Premium Loading Indicator
                    Column(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        SizedBox(
                          width: 24,
                          height: 24,
                          child: CircularProgressIndicator(
                            strokeWidth: 2.5,
                            valueColor: AlwaysStoppedAnimation<Color>(
                              Colors.white.withValues(alpha: 0.85),
                            ),
                          ),
                        ),
                        const SizedBox(height: 12),
                        Text(
                          "Loading experiences...",
                          style: TextStyle(
                            fontSize: 11,
                            fontWeight: FontWeight.w500,
                            color: Colors.white.withValues(alpha: 0.45),
                            letterSpacing: 0.8,
                          ),
                        ),
                      ],
                    ),
                    const SizedBox(height: 24),
                  ],
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }
}
