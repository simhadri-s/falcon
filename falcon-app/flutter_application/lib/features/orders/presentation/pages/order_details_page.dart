import 'package:flutter/material.dart';
import 'package:flutter_application/core/utils/price_formatter.dart';
import '../../data/order_model.dart';
import '../../data/order_service.dart';
import 'package:intl/intl.dart';
import 'return_request_page.dart';
import 'return_tracking_page.dart';
import '../../data/return_model.dart';
import '../../data/return_service.dart';
import 'package:flutter_application/core/widgets/retry_image.dart';
import 'package:flutter_application/core/utils/error_handler.dart';
import 'package:flutter_application/features/address/data/address_service.dart';

class OrderDetailsPage extends StatefulWidget {
  final Order? order;
  final String? orderId;
  const OrderDetailsPage({super.key, this.order, this.orderId});

  @override
  State<OrderDetailsPage> createState() => _OrderDetailsPageState();
}

class _OrderDetailsPageState extends State<OrderDetailsPage> {
  final OrderService _orderService = OrderService();
  final AddressService _addressService = AddressService();
  final ReturnService _returnService = ReturnService();
  bool _isUpdating = false;
  bool _isDownloadingReceipt = false;
  bool _isLoading = false;
  Order? _currentOrder;

  String _formatPrice(double value) => formatIndianPrice(value);

  @override
  void initState() {
    super.initState();
    if (widget.order != null) {
      _currentOrder = widget.order;
    } else if (widget.orderId != null) {
      _fetchOrder();
    }
  }

  Future<void> _fetchOrder() async {
    final String? id = widget.orderId ?? _currentOrder?.id;
    if (id == null) return;

    if (_currentOrder == null) {
      setState(() => _isLoading = true);
    }
    try {
      final order = await _orderService.getOrderById(id);
      if (mounted) {
        setState(() {
          _currentOrder = order;
          _isLoading = false;
        });
      }
    } catch (e) {
      if (mounted) {
        setState(() => _isLoading = false);
        ErrorHandler.showError(context, e);
      }
    }
  }

  Future<void> _refreshOrder() async {
    if (_currentOrder == null) return;
    final updated = await _orderService.getOrderById(_currentOrder!.id);
    if (updated != null && mounted) {
      setState(() {
        _currentOrder = updated;
      });
    }
  }

  Future<void> _cancelOrder() async {
    final confirm = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text("Cancel Order"),
        content: const Text("Are you sure you want to cancel this order? This action cannot be undone."),
        actions: [
          TextButton(onPressed: () => Navigator.pop(context, false), child: const Text("Keep Order")),
          TextButton(
            onPressed: () => Navigator.pop(context, true), 
            child: const Text("Cancel It", style: TextStyle(color: Colors.red)),
          ),
        ],
      ),
    );

    if (confirm != true || _currentOrder == null) return;

    setState(() => _isUpdating = true);
    final response = await _orderService.updateOrder(_currentOrder!.id, {"status": "cancelled"});
    
    if (!mounted) return;
    setState(() => _isUpdating = false);

    if (response.success) {
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text("Order cancelled successfully")));
      Navigator.pop(context, true);
    } else {
      ErrorHandler.showError(context, response.message);
    }
  }

  Future<void> _showAddressSelector() async {
    setState(() => _isUpdating = true);
    try {
      final addresses = await _addressService.getAddresses();
      if (!mounted) return;
      setState(() => _isUpdating = false);

      if (addresses.isEmpty) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text("No addresses found. Please add an address in your profile.")),
        );
        return;
      }

      final selectedId = await showModalBottomSheet<String>(
        context: context,
        isScrollControlled: true,
        shape: const RoundedRectangleBorder(
          borderRadius: BorderRadius.vertical(top: Radius.circular(24)),
        ),
        builder: (context) => DraggableScrollableSheet(
          expand: false,
          initialChildSize: 0.6,
          builder: (context, controller) => Padding(
            padding: const EdgeInsets.all(24),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text(
                  "Select New Address",
                  style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
                ),
                const SizedBox(height: 16),
                Expanded(
                  child: ListView.builder(
                    controller: controller,
                    itemCount: addresses.length,
                    itemBuilder: (context, index) {
                      final addr = addresses[index];
                      return GestureDetector(
                        onTap: () => Navigator.pop(context, addr.id),
                        child: Container(
                          margin: const EdgeInsets.only(bottom: 12),
                          padding: const EdgeInsets.all(16),
                          decoration: BoxDecoration(
                            border: Border.all(color: Colors.grey.shade200),
                            borderRadius: BorderRadius.circular(12),
                          ),
                          child: Row(
                            children: [
                              const Icon(Icons.location_on_outlined, color: Colors.deepPurple),
                              const SizedBox(width: 12),
                              Expanded(
                                child: Column(
                                  crossAxisAlignment: CrossAxisAlignment.start,
                                  children: [
                                    Text(addr.fullName, style: const TextStyle(fontWeight: FontWeight.bold)),
                                    Text("${addr.street}, ${addr.city}", style: TextStyle(color: Colors.grey.shade600, fontSize: 12)),
                                  ],
                                ),
                              ),
                              const Icon(Icons.chevron_right, color: Colors.grey),
                            ],
                          ),
                        ),
                      );
                    },
                  ),
                ),
              ],
            ),
          ),
        ),
      );

      if (selectedId != null && mounted) {
        _updateAddress(selectedId);
      }
    } catch (e) {
      if (mounted) {
        setState(() => _isUpdating = false);
        ErrorHandler.showError(context, e);
      }
    }
  }

  Future<void> _updateAddress(String addressId) async {
    setState(() => _isUpdating = true);
    final response = await _orderService.updateOrderAddress(_currentOrder!.id, addressId);
    
    if (!mounted) return;
    setState(() => _isUpdating = false);

    if (response.success) {
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(response.message)));
      _refreshOrder();
    } else {
      ErrorHandler.showError(context, response.message);
    }
  }

  Future<void> _downloadReceipt() async {
    final order = _currentOrder;
    if (order == null || _isDownloadingReceipt) return;

    setState(() => _isDownloadingReceipt = true);
    try {
      final result = await _orderService.downloadReceipt(order.id);
      if (!mounted || !result.didSave) return;

      final savedPath = result.savedPath;
      final message = (savedPath != null && savedPath.isNotEmpty)
          ? 'Receipt saved to $savedPath'
          : '${result.fileName} downloaded successfully';

      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(message)),
      );
    } catch (e) {
      if (mounted) {
        ErrorHandler.showError(context, e);
      }
    } finally {
      if (mounted) {
        setState(() => _isDownloadingReceipt = false);
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.white,
      appBar: AppBar(
        title: const Text("Order Details", style: TextStyle(fontWeight: FontWeight.bold, color: Colors.black)),
        centerTitle: true,
        backgroundColor: Colors.white,
        elevation: 0,
        iconTheme: const IconThemeData(color: Colors.black),
      ),
      body: _isLoading 
        ? const Center(child: CircularProgressIndicator())
        : _currentOrder == null
          ? const Center(child: Text("Order not found"))
          : RefreshIndicator(
              onRefresh: _fetchOrder,
              color: Colors.deepPurple,
              child: Stack(
                children: [
                  SingleChildScrollView(
                    physics: const AlwaysScrollableScrollPhysics(),
                    padding: const EdgeInsets.all(24),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        _buildOrderInfo(),
                        const SizedBox(height: 32),
                        const Text("Delivery Address", style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
                        const SizedBox(height: 16),
                        _buildAddressSection(),
                        const SizedBox(height: 32),
                        const Text("Order Status", style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
                        const SizedBox(height: 24),
                        _buildTrackingStepper(),
                        if (_currentOrder!.returnRequest != null) ...[
                          const SizedBox(height: 32),
                          const Text("Return Status", style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
                          const SizedBox(height: 16),
                          _buildReturnStatusSection(),
                        ],
                        const SizedBox(height: 32),
                        const Text("Order Items", style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
                        const SizedBox(height: 16),
                        ..._currentOrder!.items.map((item) => _buildOrderItem(item)),
                        const SizedBox(height: 8),
                        _buildPriceSummary(),
                        const SizedBox(height: 16),
                        _buildReceiptAction(),
                        const SizedBox(height: 40),
                        if (_currentOrder!.status == OrderStatus.created || _currentOrder!.status == OrderStatus.processing)
                          _buildEditActions(),
                        if (_currentOrder!.status == OrderStatus.delivered)
                          _buildReturnAction(),
                      ],
                    ),
                  ),
                  if (_isUpdating || _isDownloadingReceipt)
                    Container(
                      color: Colors.black.withOpacity(0.3),
                      child: const Center(child: CircularProgressIndicator(color: Colors.deepPurple)),
                    ),
                ],
              ),
            ),
    );
  }

  Widget _buildOrderInfo() {
    return Row(
      mainAxisAlignment: MainAxisAlignment.spaceBetween,
      children: [
        Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              "Order #${_currentOrder!.id.substring(_currentOrder!.id.length - 8).toUpperCase()}",
              style: const TextStyle(fontSize: 20, fontWeight: FontWeight.bold),
            ),
            const SizedBox(height: 4),
            Text(
              DateFormat('MMMM dd, yyyy').format(_currentOrder!.createdAt),
              style: TextStyle(color: Colors.grey.shade500),
            ),
            const SizedBox(height: 8),
            Text(
              _formatPrice(_currentOrder!.totalAmount),
              style: const TextStyle(
                fontSize: 22,
                fontWeight: FontWeight.w900,
                color: Colors.deepPurple,
              ),
            ),
          ],
        ),
        _buildStatusBadge(_currentOrder!.status),
      ],
    );
  }

  Widget _buildStatusBadge(OrderStatus status) {
    Color color;
    switch (status) {
      case OrderStatus.created: color = Colors.orange; break;
      case OrderStatus.processing: color = Colors.blue; break;
      case OrderStatus.shipped: color = Colors.purple; break;
      case OrderStatus.delivered: color = Colors.green; break;
      case OrderStatus.cancelled: color = Colors.red; break;
    }
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
      decoration: BoxDecoration(color: color.withOpacity(0.1), borderRadius: BorderRadius.circular(10)),
      child: Text(
        _currentOrder!.statusDisplay.toUpperCase(),
        style: TextStyle(color: color, fontWeight: FontWeight.bold, fontSize: 12),
      ),
    );
  }

  Widget _buildTrackingStepper() {
    final statusIndex = _currentOrder!.status == OrderStatus.cancelled ? -1 : _getStatusIndex(_currentOrder!.status);
    
    return Column(
      children: [
        _buildStep("Order Placed", "Your order has been received", statusIndex >= 0),
        _buildStep("Processing", "We are preparing your package", statusIndex >= 1),
        _buildStep("Shipped", "Your order is on the way", statusIndex >= 2),
        _buildStep("Delivered", "Package has been delivered", statusIndex >= 3, isLast: true),
      ],
    );
  }

  int _getStatusIndex(OrderStatus status) {
    switch (status) {
      case OrderStatus.created: return 0;
      case OrderStatus.processing: return 1;
      case OrderStatus.shipped: return 2;
      case OrderStatus.delivered: return 3;
      case OrderStatus.cancelled: return -1;
    }
  }

  Widget _buildStep(String title, String subtitle, bool isCompleted, {bool isLast = false}) {
    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Column(
          children: [
            Container(
              width: 24,
              height: 24,
              decoration: BoxDecoration(
                color: isCompleted ? Colors.deepPurple : Colors.grey.shade200,
                shape: BoxShape.circle,
              ),
              child: isCompleted ? const Icon(Icons.check, color: Colors.white, size: 14) : null,
            ),
            if (!isLast)
              Container(
                width: 2,
                height: 40,
                color: isCompleted ? Colors.deepPurple : Colors.grey.shade200,
              ),
          ],
        ),
        const SizedBox(width: 16),
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                title,
                style: TextStyle(
                  fontWeight: FontWeight.bold,
                  color: isCompleted ? Colors.black : Colors.grey.shade400,
                ),
              ),
              Text(
                subtitle,
                style: TextStyle(
                  fontSize: 12,
                  color: isCompleted ? Colors.grey.shade600 : Colors.grey.shade400,
                ),
              ),
              const SizedBox(height: 20),
            ],
          ),
        ),
      ],
    );
  }

  Widget _buildAddressSection() {
    final addr = _currentOrder!.address;
    if (addr == null) {
      return Container(
        padding: const EdgeInsets.all(16),
        decoration: BoxDecoration(color: Colors.grey.shade50, borderRadius: BorderRadius.circular(12)),
        child: const Text("No address information available", style: TextStyle(color: Colors.grey)),
      );
    }
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: Colors.grey.shade50,
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: Colors.grey.shade200),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(addr.fullName, style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 15)),
          const SizedBox(height: 4),
          Text(addr.phoneNumber, style: TextStyle(color: Colors.grey.shade600, fontSize: 13)),
          const SizedBox(height: 4),
          Text("${addr.street}, ${addr.city} - ${addr.pincode}", style: TextStyle(color: Colors.grey.shade600, fontSize: 13)),
          Text(addr.country, style: TextStyle(color: Colors.grey.shade600, fontSize: 13)),
        ],
      ),
    );
  }

  Widget _buildOrderItem(OrderItem item) {
    return Container(
      margin: const EdgeInsets.only(bottom: 16),
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: Colors.grey.shade50,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: Colors.grey.shade200),
      ),
      child: Row(
        children: [
          ClipRRect(
            borderRadius: BorderRadius.circular(12),
            child: item.product.imageUrls.isNotEmpty
                ? RetryNetworkImage(
                    imageUrl: item.product.imageUrls.first,
                    width: 60,
                    height: 60,
                    fit: BoxFit.cover,
                  )
                : Container(
                    width: 60,
                    height: 60,
                    color: Colors.grey.shade100,
                    child: const Icon(Icons.inventory_2_outlined, color: Colors.grey),
                  ),
          ),
          const SizedBox(width: 16),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(item.product.name, style: const TextStyle(fontWeight: FontWeight.bold)),
                if (item.variantAttributes != null && item.variantAttributes!.isNotEmpty) ...[
                  const SizedBox(height: 4),
                  Text(
                    item.variantAttributes!.values.join(' / '),
                    style: TextStyle(
                      color: Colors.grey.shade600,
                      fontSize: 12,
                      fontWeight: FontWeight.w500,
                    ),
                  ),
                ],
                const SizedBox(height: 4),
                Text("Qty: ${item.quantity}", style: TextStyle(color: Colors.grey.shade500, fontSize: 12)),
                if (item.product.hasPrice) ...[
                  const SizedBox(height: 8),
                  Row(
                    crossAxisAlignment: CrossAxisAlignment.end,
                    children: [
                      Text(
                        _formatPrice(item.product.price),
                        style: const TextStyle(
                          fontSize: 15,
                          fontWeight: FontWeight.w800,
                          color: Color(0xFF111827),
                        ),
                      ),
                      if (item.product.hasDiscount)
                        Padding(
                          padding: const EdgeInsets.only(left: 8, bottom: 1),
                          child: Text(
                            _formatPrice(item.product.mrp),
                            style: TextStyle(
                              fontSize: 12,
                              color: Colors.grey.shade500,
                              decoration: TextDecoration.lineThrough,
                            ),
                          ),
                        ),
                    ],
                  ),
                ],
              ],
            ),
          ),
          if (item.product.hasPrice)
            Text(
              _formatPrice(item.total),
              style: const TextStyle(
                fontSize: 15,
                fontWeight: FontWeight.w800,
                color: Colors.deepPurple,
              ),
            ),
        ],
      ),
    );
  }

  Widget _buildPriceSummary() {
    final double itemsSubtotal = _currentOrder!.items.fold(0.0, (sum, item) => sum + item.total);
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(18),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: Colors.grey.shade200),
      ),
      child: Column(
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              const Text(
                'Items Subtotal',
                style: TextStyle(fontSize: 14, color: Colors.grey),
              ),
              Text(
                _formatPrice(itemsSubtotal),
                style: const TextStyle(fontSize: 14, fontWeight: FontWeight.bold),
              ),
            ],
          ),
          const SizedBox(height: 8),
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              const Text(
                'Delivery Charge',
                style: TextStyle(fontSize: 14, color: Colors.grey),
              ),
              Text(
                _formatPrice(_currentOrder!.deliveryCharge),
                style: const TextStyle(fontSize: 14, fontWeight: FontWeight.bold),
              ),
            ],
          ),
          if (_currentOrder!.discountAmount > 0) ...[
            const SizedBox(height: 8),
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Text(
                  'Discount ${_currentOrder!.couponCode != null ? "(${_currentOrder!.couponCode})" : ""}',
                  style: const TextStyle(fontSize: 14, color: Colors.green, fontWeight: FontWeight.w500),
                ),
                Text(
                  '- ${_formatPrice(_currentOrder!.discountAmount)}',
                  style: const TextStyle(fontSize: 14, fontWeight: FontWeight.bold, color: Colors.green),
                ),
              ],
            ),
          ],
          const Divider(height: 24),
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              const Text(
                'Order Total',
                style: TextStyle(
                  fontSize: 17,
                  fontWeight: FontWeight.w800,
                  color: Color(0xFF111827),
                ),
              ),
              Text(
                _formatPrice(_currentOrder!.totalAmount),
                style: const TextStyle(
                  fontSize: 20,
                  fontWeight: FontWeight.w900,
                  color: Colors.deepPurple,
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildReceiptAction() {
    return SizedBox(
      width: double.infinity,
      height: 56,
      child: OutlinedButton.icon(
        onPressed: (_isUpdating || _isDownloadingReceipt) ? null : _downloadReceipt,
        icon: const Icon(Icons.download_outlined),
        label: Text(
          _isDownloadingReceipt ? 'Downloading Receipt...' : 'Download Receipt',
        ),
        style: OutlinedButton.styleFrom(
          foregroundColor: Colors.deepPurple,
          side: const BorderSide(color: Colors.deepPurple),
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(16),
          ),
        ),
      ),
    );
  }

  Widget _buildEditActions() {
    return Column(
      children: [
        SizedBox(
          width: double.infinity,
          height: 56,
          child: OutlinedButton.icon(
            onPressed: _isUpdating ? null : _showAddressSelector,
            icon: const Icon(Icons.edit_location_alt_outlined),
            label: const Text("Update Delivery Address"),
            style: OutlinedButton.styleFrom(
              foregroundColor: Colors.deepPurple,
              side: const BorderSide(color: Colors.deepPurple),
              shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
            ),
          ),
        ),
        const SizedBox(height: 16),
        SizedBox(
          width: double.infinity,
          height: 56,
          child: TextButton.icon(
            onPressed: _isUpdating ? null : _cancelOrder,
            icon: const Icon(Icons.cancel_outlined, color: Colors.red),
            label: const Text("Cancel Order", style: TextStyle(color: Colors.red, fontWeight: FontWeight.bold)),
          ),
        ),
      ],
    );
  }

  Widget _buildReturnAction() {
    if (_currentOrder?.returnRequest != null) {
      return SizedBox(
        width: double.infinity,
        height: 56,
        child: ElevatedButton.icon(
          onPressed: () {
            Navigator.push(
              context,
              MaterialPageRoute(builder: (context) => ReturnTrackingPage(returnRequest: _currentOrder!.returnRequest!)),
            );
          },
          icon: const Icon(Icons.track_changes_outlined),
          label: const Text("Track Return", style: TextStyle(fontWeight: FontWeight.bold)),
          style: ElevatedButton.styleFrom(
            backgroundColor: Colors.blue.shade700,
            foregroundColor: Colors.white,
            shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
            elevation: 0,
          ),
        ),
      );
    }

    return SizedBox(
      width: double.infinity,
      height: 56,
      child: ElevatedButton.icon(
        onPressed: () async {
          final result = await Navigator.push(
            context,
            MaterialPageRoute(builder: (context) => ReturnRequestPage(order: _currentOrder!)),
          );
          if (result == true) {
            _refreshOrder();
          }
        },
        icon: const Icon(Icons.assignment_return_outlined),
        label: const Text("Return Items", style: TextStyle(fontWeight: FontWeight.bold)),
        style: ElevatedButton.styleFrom(
          backgroundColor: Colors.orange.shade800,
          foregroundColor: Colors.white,
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
          elevation: 0,
        ),
      ),
    );
  }

  Widget _buildReturnStatusSection() {
    final ret = _currentOrder!.returnRequest!;
    final status = ret.status;

    Color statusColor;
    IconData statusIcon;
    String statusText = ret.statusDisplay;

    switch (status) {
      case ReturnStatus.returnRequested:
        statusColor = Colors.orange;
        statusIcon = Icons.hourglass_empty;
        break;
      case ReturnStatus.returnApproved:
        statusColor = Colors.blue;
        statusIcon = Icons.check_circle_outline;
        break;
      case ReturnStatus.returnRejected:
        statusColor = Colors.red;
        statusIcon = Icons.cancel_outlined;
        break;
      case ReturnStatus.returnPickedUp:
        statusColor = Colors.purple;
        statusIcon = Icons.local_shipping_outlined;
        break;
      case ReturnStatus.returnCompleted:
        statusColor = Colors.green;
        statusIcon = Icons.verified_outlined;
        break;
    }

    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: statusColor.withOpacity(0.05),
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: statusColor.withOpacity(0.2)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Icon(statusIcon, color: statusColor, size: 20),
              const SizedBox(width: 12),
              Text(
                statusText,
                style: TextStyle(color: statusColor, fontWeight: FontWeight.bold, fontSize: 16),
              ),
            ],
          ),
          if (ret.adminComment != null && ret.adminComment!.isNotEmpty) ...[
            const SizedBox(height: 12),
            const Text(
              "Admin Feedback:",
              style: TextStyle(fontWeight: FontWeight.bold, fontSize: 12, color: Colors.black54),
            ),
            const SizedBox(height: 4),
            Text(
              ret.adminComment!,
              style: const TextStyle(fontSize: 14, color: Colors.black87),
            ),
          ],
          const SizedBox(height: 16),
          _buildReturnTimeline(),
        ],
      ),
    );
  }

  Widget _buildReturnTimeline() {
    final ret = _currentOrder!.returnRequest!;
    final status = ret.status;
    int currentIndex = 0;

    if (status == ReturnStatus.returnRejected) {
      currentIndex = -1; // Special case
    } else {
      switch (status) {
        case ReturnStatus.returnRequested: currentIndex = 0; break;
        case ReturnStatus.returnApproved: currentIndex = 1; break;
        case ReturnStatus.returnPickedUp: currentIndex = 2; break;
        case ReturnStatus.returnCompleted: currentIndex = 3; break;
        default: currentIndex = 0;
      }
    }

    return Column(
      children: [
        _buildReturnStep("Requested", currentIndex >= 0, isFirst: true),
        if (status == ReturnStatus.returnRejected)
          _buildReturnStep("Rejected", true, color: Colors.red, isLast: true)
        else ...[
          _buildReturnStep("Approved", currentIndex >= 1),
          _buildReturnStep("Picked Up", currentIndex >= 2),
          _buildReturnStep("Refund Completed", currentIndex >= 3, isLast: true),
        ],
      ],
    );
  }

  Widget _buildReturnStep(String title, bool isCompleted, {bool isFirst = false, bool isLast = false, Color color = Colors.green}) {
    return Row(
      children: [
        Column(
          children: [
            Container(
              width: 12,
              height: 12,
              decoration: BoxDecoration(
                color: isCompleted ? color : Colors.grey.shade300,
                shape: BoxShape.circle,
              ),
            ),
            if (!isLast)
              Container(
                width: 2,
                height: 20,
                color: isCompleted ? color : Colors.grey.shade300,
              ),
          ],
        ),
        const SizedBox(width: 12),
        Padding(
          padding: EdgeInsets.only(bottom: isLast ? 0 : 20),
          child: Text(
            title,
            style: TextStyle(
              fontSize: 12,
              fontWeight: isCompleted ? FontWeight.bold : FontWeight.normal,
              color: isCompleted ? Colors.black87 : Colors.grey.shade400,
            ),
          ),
        ),
      ],
    );
  }
}

