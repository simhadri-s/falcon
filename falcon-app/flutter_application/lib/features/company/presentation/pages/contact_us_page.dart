import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:provider/provider.dart';
import 'package:url_launcher/url_launcher.dart';
import 'package:flutter_application/core/widgets/retry_image.dart';
import 'package:flutter_application/features/company/presentation/providers/company_provider.dart';

class ContactUsPage extends StatelessWidget {
  const ContactUsPage({super.key});

  void _copyToClipboard(BuildContext context, String label, String value) {
    if (value.isEmpty) return;
    Clipboard.setData(ClipboardData(text: value));
    
    ScaffoldMessenger.of(context).clearSnackBars();
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Row(
          children: [
            const Icon(Icons.check_circle_outline, color: Colors.white, size: 20),
            const SizedBox(width: 12),
            Expanded(
              child: Text(
                '$label copied to clipboard!',
                style: const TextStyle(fontWeight: FontWeight.w500),
              ),
            ),
          ],
        ),
        behavior: SnackBarBehavior.floating,
        backgroundColor: Colors.deepPurple.shade700,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
        margin: const EdgeInsets.all(16),
        duration: const Duration(seconds: 2),
      ),
    );
  }

  Future<void> _launchUrl(BuildContext context, String urlString, String errorMessage) async {
    final Uri url = Uri.parse(urlString);
    try {
      // Try launching directly to bypass OS-level canLaunchUrl restrictions for specific schemes like mailto/tel
      final bool launched = await launchUrl(url);
      if (!launched) {
        throw 'Could not launch $urlString';
      }
    } catch (e) {
      if (context.mounted) {
        ScaffoldMessenger.of(context).clearSnackBars();
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Row(
              children: [
                const Icon(Icons.error_outline, color: Colors.white, size: 20),
                const SizedBox(width: 12),
                Expanded(
                  child: Text(
                    errorMessage,
                    style: const TextStyle(fontWeight: FontWeight.w500),
                  ),
                ),
              ],
            ),
            behavior: SnackBarBehavior.floating,
            backgroundColor: Colors.red.shade700,
            shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
            margin: const EdgeInsets.all(16),
          ),
        );
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text(
          'Contact Us',
          style: TextStyle(fontWeight: FontWeight.bold),
        ),
        backgroundColor: Colors.transparent,
        elevation: 0,
        foregroundColor: Colors.black87,
        flexibleSpace: Container(
          decoration: const BoxDecoration(
            gradient: LinearGradient(
              colors: [Colors.white, Colors.white],
              begin: Alignment.topCenter,
              end: Alignment.bottomCenter,
            ),
          ),
        ),
      ),
      body: Consumer<CompanyProvider>(
        builder: (context, company, child) {
          return SingleChildScrollView(
            physics: const BouncingScrollPhysics(),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                // Top Header Section with Gradient Card
                _buildHeaderCard(context, company),
                
                const SizedBox(height: 24),
                
                // Contact Details Section
                Padding(
                  padding: const EdgeInsets.symmetric(horizontal: 20),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      const Text(
                        'Get in Touch',
                        style: TextStyle(
                          fontSize: 20,
                          fontWeight: FontWeight.bold,
                          color: Colors.black87,
                          letterSpacing: 0.5,
                        ),
                      ),
                      const SizedBox(height: 8),
                      Text(
                        'Reach out to us through any of the channels below. Tap on a card to open it directly!',
                        style: TextStyle(
                          fontSize: 14,
                          color: Colors.grey.shade600,
                          height: 1.4,
                        ),
                      ),
                      const SizedBox(height: 20),
                      
                      // Address Card
                      if (company.address.isNotEmpty)
                        _buildContactCard(
                          context,
                          icon: Icons.location_on_outlined,
                          iconColor: Colors.red.shade600,
                          title: 'Our Office',
                          value: company.address,
                          onTap: () {
                            final encodedAddress = Uri.encodeComponent(company.address);
                            _launchUrl(
                              context, 
                              'https://www.google.com/maps/search/?api=1&query=$encodedAddress', 
                              'Could not open maps for ${company.address}'
                            );
                          },
                        ),
                      
                      // Phone Card
                      if (company.phone.isNotEmpty)
                        _buildContactCard(
                          context,
                          icon: Icons.phone_outlined,
                          iconColor: Colors.green.shade600,
                          title: 'Phone Number',
                          value: company.phone,
                          onTap: () {
                            final cleanPhone = company.phone.replaceAll(RegExp(r'[^\d+]'), '');
                            _launchUrl(
                              context, 
                              'tel:$cleanPhone', 
                              'Could not open the dialer for ${company.phone}'
                            );
                          },
                        ),
                      
                      // Email Card
                      if (company.email.isNotEmpty)
                        _buildContactCard(
                          context,
                          icon: Icons.mail_outline,
                          iconColor: Colors.blue.shade600,
                          title: 'Email Address',
                          value: company.email,
                          onTap: () {
                            _launchUrl(
                              context, 
                              'mailto:${company.email}', 
                              'Could not open the mail application for ${company.email}'
                            );
                          },
                        ),
                      
                      // Working Hours Card
                      if (company.workingHours.isNotEmpty)
                        _buildContactCard(
                          context,
                          icon: Icons.access_time_outlined,
                          iconColor: Colors.orange.shade600,
                          title: 'Working Hours',
                          value: company.workingHours,
                          actionIcon: null, // No copy option needed for working hours
                        ),
                    ],
                  ),
                ),
                const SizedBox(height: 40),
              ],
            ),
          );
        },
      ),
    );
  }

  Widget _buildHeaderCard(BuildContext context, CompanyProvider company) {
    return Container(
      margin: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(24),
        gradient: LinearGradient(
          colors: [Colors.deepPurple.shade800, Colors.deepPurple.shade500],
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
        ),
        boxShadow: [
          BoxShadow(
            color: Colors.deepPurple.shade300.withOpacity(0.4),
            blurRadius: 15,
            offset: const Offset(0, 8),
          ),
        ],
      ),
      child: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          children: [
            // Company Logo with glowing outline
            Container(
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                border: Border.all(color: Colors.white24, width: 4),
                boxShadow: [
                  BoxShadow(
                    color: Colors.black12,
                    blurRadius: 8,
                    offset: const Offset(0, 4),
                  ),
                ],
              ),
              child: ClipOval(
                child: Container(
                  color: Colors.white,
                  padding: const EdgeInsets.all(4),
                  child: company.iconUrl.isNotEmpty || company.logoUrl.isNotEmpty
                      ? ClipOval(
                          child: RetryNetworkImage(
                            imageUrl: company.iconUrl.isNotEmpty ? company.iconUrl : company.logoUrl,
                            width: 80,
                            height: 80,
                            fit: BoxFit.cover,
                          ),
                        )
                      : CircleAvatar(
                          radius: 40,
                          backgroundColor: Colors.grey.shade50,
                          child: Icon(Icons.store, color: Colors.deepPurple.shade700, size: 40),
                        ),
                ),
              ),
            ),
            const SizedBox(height: 18),
            
            // Company Name
            Text(
              company.companyName,
              textAlign: TextAlign.center,
              style: const TextStyle(
                fontSize: 24,
                fontWeight: FontWeight.bold,
                color: Colors.white,
                letterSpacing: 0.5,
              ),
            ),
            
            // Company Description
            if (company.companyDescription.isNotEmpty) ...[
              const SizedBox(height: 12),
              Padding(
                padding: const EdgeInsets.symmetric(horizontal: 10),
                child: Text(
                  company.companyDescription,
                  textAlign: TextAlign.center,
                  style: TextStyle(
                    fontSize: 14,
                    color: Colors.white.withOpacity(0.85),
                    height: 1.4,
                  ),
                ),
              ),
            ],
          ],
        ),
      ),
    );
  }

  Widget _buildContactCard(
    BuildContext context, {
    required IconData icon,
    required Color iconColor,
    required String title,
    required String value,
    VoidCallback? onTap,
    VoidCallback? onActionTap,
    IconData? actionIcon,
    String? actionTooltip,
  }) {
    return Container(
      margin: const EdgeInsets.only(bottom: 16),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: Colors.grey.shade100, width: 1),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withOpacity(0.02),
            blurRadius: 10,
            offset: const Offset(0, 4),
          ),
        ],
      ),
      child: ClipRRect(
        borderRadius: BorderRadius.circular(16),
        child: InkWell(
          onTap: onTap,
          splashColor: iconColor.withOpacity(0.1),
          highlightColor: iconColor.withOpacity(0.05),
          child: Padding(
            padding: const EdgeInsets.all(16),
            child: Row(
              children: [
                // Left colored icon circle
                Container(
                  padding: const EdgeInsets.all(12),
                  decoration: BoxDecoration(
                    color: iconColor.withOpacity(0.1),
                    shape: BoxShape.circle,
                  ),
                  child: Icon(
                    icon,
                    color: iconColor,
                    size: 24,
                  ),
                ),
                const SizedBox(width: 16),
                
                // Text Details
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        title,
                        style: TextStyle(
                          fontSize: 12,
                          fontWeight: FontWeight.bold,
                          color: Colors.grey.shade500,
                          textBaseline: TextBaseline.alphabetic,
                        ),
                      ),
                      const SizedBox(height: 4),
                      Text(
                        value,
                        style: const TextStyle(
                          fontSize: 15,
                          fontWeight: FontWeight.w600,
                          color: Colors.black87,
                          height: 1.3,
                        ),
                      ),
                    ],
                  ),
                ),
                
                // Optional Action Button (e.g. Copy)
                if (actionIcon != null && onActionTap != null) ...[
                  const SizedBox(width: 8),
                  IconButton(
                    onPressed: onActionTap,
                    icon: Icon(actionIcon, size: 20),
                    color: Colors.grey.shade400,
                    tooltip: actionTooltip,
                    splashRadius: 20,
                  ),
                ],
              ],
            ),
          ),
        ),
      ),
    );
  }
}
