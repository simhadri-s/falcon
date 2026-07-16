import 'package:flutter/material.dart';
import '../../data/order_model.dart';
import '../../data/return_service.dart';
import 'package:flutter_application/core/utils/error_handler.dart';
import 'package:flutter_application/core/widgets/retry_image.dart';

class ReturnRequestPage extends StatefulWidget {
  final Order order;
  const ReturnRequestPage({super.key, required this.order});

  @override
  State<ReturnRequestPage> createState() => _ReturnRequestPageState();
}

class _ReturnRequestPageState extends State<ReturnRequestPage> {
  final ReturnService _returnService = ReturnService();
  final Map<String, int> _selectedItems = {};
  final List<String> _reasons = [
    'Defective/Damaged product',
    'Received wrong item',
    'Quality not as expected',
    'Part/Accessory missing',
    'Size doesn\'t fit',
    'Changed my mind',
    'Other'
  ];
  String? _selectedReason;
  final TextEditingController _commentController = TextEditingController();
  bool _isSubmitting = false;

  @override
  void initState() {
    super.initState();
    // Initially select all items with full quantity?
    // No, better let user select.
  }

  void _toggleItem(OrderItem item) {
    setState(() {
      if (_selectedItems.containsKey(item.id)) {
        _selectedItems.remove(item.id);
      } else {
        _selectedItems[item.id] = item.quantity;
      }
    });
  }

  void _updateQuantity(OrderItem item, int qty) {
    if (qty <= 0) {
      _toggleItem(item);
      return;
    }
    if (qty > item.quantity) return;
    setState(() {
      _selectedItems[item.id] = qty;
    });
  }

  Future<void> _submitReturn() async {
    if (_selectedItems.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text("Please select at least one item to return")));
      return;
    }
    if (_selectedReason == null) {
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text("Please select a reason for return")));
      return;
    }

    setState(() => _isSubmitting = true);
    try {
      final items = _selectedItems.entries.map((e) => {
        'orderItemId': e.key,
        'quantity': e.value,
      }).toList();

      await _returnService.createReturnRequest(
        orderId: widget.order.id,
        items: items,
        reason: _selectedReason!,
        comment: _commentController.text,
      );

      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text("Return request submitted successfully")));
        Navigator.pop(context, true);
      }
    } catch (e) {
      if (mounted) {
        setState(() => _isSubmitting = false);
        ErrorHandler.showError(context, e);
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.white,
      appBar: AppBar(
        title: const Text("Return Order", style: TextStyle(fontWeight: FontWeight.bold, color: Colors.black)),
        centerTitle: true,
        backgroundColor: Colors.white,
        elevation: 0,
        iconTheme: const IconThemeData(color: Colors.black),
      ),
      body: Stack(
        children: [
          SingleChildScrollView(
            padding: const EdgeInsets.all(24),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text("Select items to return", style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
                const SizedBox(height: 16),
                ...widget.order.items.map((item) => _buildItemTile(item)),
                const SizedBox(height: 32),
                const Text("Reason for return", style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
                const SizedBox(height: 16),
                _buildReasonDropdown(),
                const SizedBox(height: 24),
                const Text("Comments (Optional)", style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
                const SizedBox(height: 16),
                _buildCommentField(),
                const SizedBox(height: 100), // Space for bottom button
              ],
            ),
          ),
          if (_isSubmitting)
            Container(
              color: Colors.black.withOpacity(0.3),
              child: const Center(child: CircularProgressIndicator(color: Colors.deepPurple)),
            ),
        ],
      ),
      bottomSheet: Container(
        padding: const EdgeInsets.all(24),
        decoration: BoxDecoration(
          color: Colors.white,
          boxShadow: [BoxShadow(color: Colors.black.withOpacity(0.05), blurRadius: 10, offset: const Offset(0, -5))],
        ),
        child: SizedBox(
          width: double.infinity,
          height: 56,
          child: ElevatedButton(
            onPressed: _isSubmitting ? null : _submitReturn,
            style: ElevatedButton.styleFrom(
              backgroundColor: Colors.deepPurple,
              foregroundColor: Colors.white,
              shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
              elevation: 0,
            ),
            child: const Text("Submit Return Request", style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16)),
          ),
        ),
      ),
    );
  }

  Widget _buildItemTile(OrderItem item) {
    final isSelected = _selectedItems.containsKey(item.id);
    final currentQty = _selectedItems[item.id] ?? 0;

    return Container(
      margin: const EdgeInsets.only(bottom: 12),
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: isSelected ? Colors.deepPurple.withOpacity(0.05) : Colors.grey.shade50,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: isSelected ? Colors.deepPurple : Colors.grey.shade200),
      ),
      child: Row(
        children: [
          Checkbox(
            value: isSelected,
            onChanged: (_) => _toggleItem(item),
            activeColor: Colors.deepPurple,
          ),
          ClipRRect(
            borderRadius: BorderRadius.circular(8),
            child: item.product.imageUrls.isNotEmpty
                ? RetryNetworkImage(
                    imageUrl: item.product.imageUrls.first,
                    width: 50,
                    height: 50,
                    fit: BoxFit.cover,
                  )
                : Container(width: 50, height: 50, color: Colors.grey.shade100),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(item.product.name, style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 14), maxLines: 1, overflow: TextOverflow.ellipsis),
                Text("Ordered: ${item.quantity}", style: TextStyle(color: Colors.grey.shade500, fontSize: 11)),
              ],
            ),
          ),
          if (isSelected)
            Row(
              children: [
                _qtyBtn(Icons.remove, () => _updateQuantity(item, currentQty - 1)),
                Padding(
                  padding: const EdgeInsets.symmetric(horizontal: 12),
                  child: Text("$currentQty", style: const TextStyle(fontWeight: FontWeight.bold)),
                ),
                _qtyBtn(Icons.add, () => _updateQuantity(item, currentQty + 1)),
              ],
            ),
        ],
      ),
    );
  }

  Widget _qtyBtn(IconData icon, VoidCallback onTap) {
    return GestureDetector(
      onTap: onTap,
      child: Container(
        padding: const EdgeInsets.all(4),
        decoration: BoxDecoration(color: Colors.white, borderRadius: BorderRadius.circular(8), border: Border.all(color: Colors.grey.shade300)),
        child: Icon(icon, size: 16),
      ),
    );
  }

  Widget _buildReasonDropdown() {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 16),
      decoration: BoxDecoration(
        color: Colors.grey.shade50,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: Colors.grey.shade200),
      ),
      child: DropdownButtonHideUnderline(
        child: DropdownButton<String>(
          value: _selectedReason,
          isExpanded: true,
          hint: const Text("Select a reason"),
          items: _reasons.map((r) => DropdownMenuItem(value: r, child: Text(r))).toList(),
          onChanged: (val) => setState(() => _selectedReason = val),
        ),
      ),
    );
  }

  Widget _buildCommentField() {
    return TextField(
      controller: _commentController,
      maxLines: 4,
      decoration: InputDecoration(
        hintText: "Write more details about why you want to return...",
        fillColor: Colors.grey.shade50,
        filled: true,
        border: OutlineInputBorder(borderRadius: BorderRadius.circular(16), borderSide: BorderSide.none),
      ),
    );
  }
}
