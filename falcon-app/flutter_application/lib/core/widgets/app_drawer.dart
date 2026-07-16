import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../features/company/presentation/providers/company_provider.dart';
import '../../core/widgets/retry_image.dart';
import '../../features/news/presentation/pages/news_list_page.dart';
import '../../features/inquiry/presentation/pages/inquiry_page.dart';
import '../../features/jobs/presentation/pages/jobs_page.dart';
import '../../features/products/presentation/pages/categories_page.dart';
import '../../features/wishlist/presentation/pages/wishlist_page.dart';
import '../../core/navigation/main_nav.dart';
import 'package:flutter_application/features/company/presentation/pages/contact_us_page.dart';

class AppDrawer extends StatelessWidget {
  const AppDrawer({super.key});

  @override
  Widget build(BuildContext context) {
    return Drawer(
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.zero,
      ),
      child: Column(
        children: [
          _buildHeader(context),
          Expanded(
            child: ListView(
              padding: EdgeInsets.zero,
              children: [
                _buildDrawerItem(
                  icon: Icons.home_outlined,
                  title: 'Home',
                  onTap: () {
                    // This now correctly takes you back to the Home dashboard
                    Navigator.of(context).pushAndRemoveUntil(
                      MaterialPageRoute(builder: (context) => MainNav()),
                      (route) => false,
                    );
                  },
                ),
                _buildDrawerItem(
                  icon: Icons.category_outlined,
                  title: 'Categories',
                  onTap: () {
                    Navigator.pop(context);
                    Navigator.push(
                      context,
                      MaterialPageRoute(builder: (context) => const CategoriesPage()),
                    );
                  },
                ),
                _buildDrawerItem(
                  icon: Icons.favorite_border,
                  title: 'Wishlist',
                  onTap: () {
                    Navigator.pop(context);
                    Navigator.push(
                      context,
                      MaterialPageRoute(builder: (context) => const WishlistPage()),
                    );
                  },
                ),
                _buildDrawerItem(
                  icon: Icons.newspaper_outlined,
                  title: 'News Section',
                  onTap: () {
                    Navigator.pop(context);
                    Navigator.push(
                      context,
                      MaterialPageRoute(builder: (context) => const NewsListPage()),
                    );
                  },
                ),
                _buildDrawerItem(
                  icon: Icons.contact_support_outlined,
                  title: 'Inquiry',
                  onTap: () {
                    Navigator.pop(context);
                    Navigator.push(
                      context,
                      MaterialPageRoute(builder: (context) => const InquiryPage()),
                    );
                  },
                ),
                _buildDrawerItem(
                  icon: Icons.work_outline,
                  title: 'Jobs',
                  onTap: () {
                    Navigator.pop(context);
                    Navigator.push(
                      context,
                      MaterialPageRoute(builder: (context) => const JobsPage()),
                    );
                  },
                ),
                _buildDrawerItem(
                  icon: Icons.phone_outlined,
                  title: 'Contact Us',
                  onTap: () {
                    Navigator.pop(context);
                    Navigator.push(
                      context,
                      MaterialPageRoute(builder: (context) => const ContactUsPage()),
                    );
                  },
                ),
              ],
            ),
          ),
          Container(
            width: double.infinity,
            padding: const EdgeInsets.symmetric(vertical: 18.0, horizontal: 20.0),
            decoration: BoxDecoration(
              color: Colors.white,
              border: Border(
                top: BorderSide(
                  color: Colors.grey.shade100,
                  width: 1.0,
                ),
              ),
              boxShadow: [
                BoxShadow(
                  color: Colors.black.withValues(alpha: 0.04),
                  blurRadius: 10,
                  offset: const Offset(0, -5), // Feathered shadow going upward
                ),
              ],
            ),
            child: Center(
              child: Consumer<CompanyProvider>(
                builder: (context, company, child) {
                  return Text(
                    'v1.0.0 | ${company.companyName}',
                    style: TextStyle(
                      color: Colors.grey.shade500, 
                      fontSize: 12,
                      fontWeight: FontWeight.w600,
                      letterSpacing: 0.3,
                    ),
                  );
                },
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildHeader(BuildContext context) {
    return Consumer<CompanyProvider>(
      builder: (context, company, child) {
        return Container(
          width: double.infinity,
          padding: const EdgeInsets.only(top: 60, bottom: 20, left: 20, right: 20),
          decoration: BoxDecoration(
            gradient: LinearGradient(
              colors: [Colors.deepPurple.shade700, Colors.deepPurple.shade500],
              begin: Alignment.topLeft,
              end: Alignment.bottomRight,
            ),
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              if (company.iconUrl.isNotEmpty || company.logoUrl.isNotEmpty)
                ClipOval(
                  child: RetryNetworkImage(
                    imageUrl: company.iconUrl.isNotEmpty ? company.iconUrl : company.logoUrl,
                    width: 60,
                    height: 60,
                    fit: BoxFit.cover,
                  ),
                )
              else
                const CircleAvatar(
                  radius: 30,
                  backgroundColor: Colors.white,
                  child: Icon(Icons.store, color: Colors.deepPurple, size: 30),
                ),
              const SizedBox(height: 16),
              Text(
                company.companyName,
                style: const TextStyle(
                  color: Colors.white,
                  fontSize: 20,
                  fontWeight: FontWeight.bold,
                ),
              ),
              if (company.email.isNotEmpty)
                Text(
                  company.email,
                  style: TextStyle(
                    color: Colors.white.withOpacity(0.8),
                    fontSize: 14,
                  ),
                ),
            ],
          ),
        );
      },
    );
  }

  Widget _buildDrawerItem({
    required IconData icon,
    required String title,
    required VoidCallback onTap,
  }) {
    return Column(
      mainAxisSize: MainAxisSize.min,
      children: [
        ListTile(
          leading: Icon(icon, color: Colors.deepPurple.shade700, size: 23),
          title: Text(
            title,
            style: const TextStyle(
              fontSize: 17, 
              fontWeight: FontWeight.w600,
              color: Colors.black87,
            ),
          ),
          onTap: onTap,
          dense: true,
          contentPadding: const EdgeInsets.symmetric(horizontal: 20, vertical: 2),
        ),
        Divider(
          color: Colors.grey.shade200,
          height: 8,
          thickness: 1.0,
          indent: 20,
          endIndent: 20,
        ),
      ],
    );
  }
}
