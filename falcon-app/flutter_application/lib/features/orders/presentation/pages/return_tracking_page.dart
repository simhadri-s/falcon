import 'package:flutter/material.dart';
import '../../data/return_model.dart';
import 'package:intl/intl.dart';

class ReturnTrackingPage extends StatelessWidget {
  final ReturnRequest returnRequest;
  const ReturnTrackingPage({super.key, required this.returnRequest});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.white,
      appBar: AppBar(
        title: const Text("Track Return", style: TextStyle(fontWeight: FontWeight.bold, color: Colors.black)),
        centerTitle: true,
        backgroundColor: Colors.white,
        elevation: 0,
        iconTheme: const IconThemeData(color: Colors.black),
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(24),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            _buildReturnSummary(),
            const SizedBox(height: 32),
            const Text("Return Progress", style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
            const SizedBox(height: 24),
            _buildTrackingStepper(),
            const SizedBox(height: 32),
            const Text("Items Returning", style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
            const SizedBox(height: 16),
            ...returnRequest.items.map((item) => _buildItemTile(item)),
            if (returnRequest.adminComment != null && returnRequest.adminComment!.isNotEmpty) ...[
              const SizedBox(height: 32),
              const Text("Admin Notes", style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
              const SizedBox(height: 12),
              Container(
                width: double.infinity,
                padding: const EdgeInsets.all(16),
                decoration: BoxDecoration(color: Colors.blue.withOpacity(0.05), borderRadius: BorderRadius.circular(12), border: Border.all(color: Colors.blue.withOpacity(0.1))),
                child: Text(returnRequest.adminComment!, style: const TextStyle(color: Colors.blueGrey, fontStyle: FontStyle.italic)),
              ),
            ],
          ],
        ),
      ),
    );
  }

  Widget _buildReturnSummary() {
    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: Colors.deepPurple.withOpacity(0.05),
        borderRadius: BorderRadius.circular(20),
        border: Border.all(color: Colors.deepPurple.withOpacity(0.1)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text("Request ID", style: TextStyle(color: Colors.grey.shade600, fontSize: 12, fontWeight: FontWeight.bold, letterSpacing: 1)),
              Text(DateFormat('MMM dd, yyyy').format(returnRequest.createdAt), style: TextStyle(color: Colors.grey.shade600, fontSize: 12)),
            ],
          ),
          const SizedBox(height: 4),
          Text("#${returnRequest.id.substring(returnRequest.id.length - 8).toUpperCase()}", style: const TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
          const Divider(height: 32),
          Row(
            children: [
              const Icon(Icons.info_outline, size: 16, color: Colors.deepPurple),
              const SizedBox(width: 8),
              Expanded(child: Text(returnRequest.reason, style: const TextStyle(fontWeight: FontWeight.w500))),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildTrackingStepper() {
    final statusIndex = _getStatusIndex(returnRequest.status);
    final isRejected = returnRequest.status == ReturnStatus.returnRejected;
    
    return Column(
      children: [
        _buildStep("Requested", "We received your return request", statusIndex >= 0),
        if (isRejected)
          _buildStep("Rejected", "Your return request was rejected", true, color: Colors.red, isLast: true)
        else ...[
          _buildStep("Approved", "Return has been approved", statusIndex >= 1),
          _buildStep("Picked Up", "Items have been picked up", statusIndex >= 3),
          _buildStep("Refund Completed", "Amount has been refunded", statusIndex >= 4, isLast: true),
        ],
      ],
    );
  }

  int _getStatusIndex(ReturnStatus status) {
    switch (status) {
      case ReturnStatus.returnRequested: return 0;
      case ReturnStatus.returnApproved: return 1;
      case ReturnStatus.returnRejected: return -1;
      case ReturnStatus.returnPickedUp: return 3;
      case ReturnStatus.returnCompleted: return 4;
    }
  }

  Widget _buildStep(String title, String subtitle, bool isCompleted, {bool isLast = false, Color color = Colors.green}) {
    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Column(
          children: [
            Container(
              width: 24,
              height: 24,
              decoration: BoxDecoration(
                color: isCompleted ? color : Colors.grey.shade200,
                shape: BoxShape.circle,
              ),
              child: isCompleted ? const Icon(Icons.check, color: Colors.white, size: 14) : null,
            ),
            if (!isLast)
              Container(
                width: 2,
                height: 40,
                color: isCompleted ? color : Colors.grey.shade200,
              ),
          ],
        ),
        const SizedBox(width: 16),
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(title, style: TextStyle(fontWeight: FontWeight.bold, color: isCompleted ? Colors.black : Colors.grey.shade400)),
              Text(subtitle, style: TextStyle(fontSize: 12, color: isCompleted ? Colors.grey.shade600 : Colors.grey.shade400)),
              const SizedBox(height: 20),
            ],
          ),
        ),
      ],
    );
  }

  Widget _buildItemTile(ReturnItem item) {
    return Container(
      margin: const EdgeInsets.only(bottom: 12),
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(color: Colors.grey.shade50, borderRadius: BorderRadius.circular(16), border: Border.all(color: Colors.grey.shade200)),
      child: Row(
        children: [
          ClipRRect(
            borderRadius: BorderRadius.circular(8),
            child: item.product.imageUrls.isNotEmpty
                ? Image.network(item.product.imageUrls.first, width: 40, height: 40, fit: BoxFit.cover)
                : Container(width: 40, height: 40, color: Colors.grey.shade100),
          ),
          const SizedBox(width: 12),
          Expanded(child: Text(item.product.name, style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 13))),
          Text("Qty: ${item.quantity}", style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 13, color: Colors.deepPurple)),
        ],
      ),
    );
  }
}
