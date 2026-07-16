import 'dart:convert';
import 'package:flutter_application/core/network/api_client.dart';
import 'package:flutter_application/core/constants/api_constants.dart';
import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:flutter_application/core/widgets/retry_image.dart';
import 'package:flutter_application/features/cart/presentation/providers/cart_provider.dart';
import 'package:flutter_application/features/orders/data/order_service.dart';
import 'package:flutter_application/features/address/data/address_service.dart';
import 'package:flutter_application/features/address/data/address_model.dart';
import 'package:flutter_application/core/utils/error_handler.dart';
import 'package:flutter_application/features/coupon/data/coupon_service.dart';

import 'package:flutter_application/features/cart/data/cart_item_model.dart';
import 'package:flutter_application/features/orders/presentation/pages/order_success_page.dart';

class CheckoutPage extends StatefulWidget {
  final CartItem? buyNowItem;
  const CheckoutPage({super.key, this.buyNowItem});

  @override
  State<CheckoutPage> createState() => _CheckoutPageState();
}

class _CheckoutPageState extends State<CheckoutPage> {
  final OrderService _orderService = OrderService();
  final AddressService _addressService = AddressService();
  final CouponService _couponService = CouponService();

  bool _isProcessing = false;
  bool _isLoadingAddresses = true;
  List<Address> _addresses = [];
  Address? _selectedAddress;

  double _deliveryCharge = 0.0;
  bool _isLoadingDeliveryCharge = false;

  // Coupon state
  final TextEditingController _couponController = TextEditingController();
  bool _isValidatingCoupon = false;
  double _discountAmount = 0.0;
  String? _appliedCouponCode;
  String? _couponMessage;
  bool _couponApplied = false;

  String _formatPrice(double value) => '\u20B9${value.toStringAsFixed(0)}';

  @override
  void initState() {
    super.initState();
    _fetchAddresses();
  }

  @override
  void dispose() {
    _couponController.dispose();
    super.dispose();
  }

  Future<void> _fetchDeliveryCharge() async {
    if (_selectedAddress == null || _selectedAddress!.pincode.isEmpty) {
      setState(() {
        _deliveryCharge = 0.0;
      });
      return;
    }

    setState(() => _isLoadingDeliveryCharge = true);
    try {
      final pincode = _selectedAddress!.pincode.trim();
      final url =
          '${ApiConstants.baseUrl}/delivery-location?search=$pincode&limit=1';
      final response = await ApiClient.get(Uri.parse(url));

      if (response.statusCode == 200) {
        final decoded = json.decode(response.body);
        final List<dynamic> data = decoded['data'] ?? [];
        if (data.isNotEmpty) {
          final charge = data.first['deliveryCharge'];
          setState(() {
            _deliveryCharge = (charge is num) ? charge.toDouble() : 0.0;
          });
        } else {
          setState(() {
            _deliveryCharge = 0.0;
          });
        }
      } else {
        setState(() {
          _deliveryCharge = 0.0;
        });
      }
    } catch (e) {
      debugPrint('Error fetching delivery charge: $e');
      setState(() {
        _deliveryCharge = 0.0;
      });
    } finally {
      if (mounted) {
        setState(() => _isLoadingDeliveryCharge = false);
      }
    }
  }

  Future<void> _applyCoupon(double orderTotal) async {
    final code = _couponController.text.trim();
    if (code.isEmpty) return;

    setState(() {
      _isValidatingCoupon = true;
      _couponMessage = null;
    });

    final cart = context.read<CartProvider>();
    final List<CartItem> itemsToOrder = widget.buyNowItem != null
        ? [widget.buyNowItem!]
        : cart.activeItems;

    final cartItems = itemsToOrder.map((item) => {
      'productId': item.product.id,
      'categoryId': item.product.categoryId,
      'price': item.price,
      'quantity': item.quantity,
      if (item.variantId != null) 'variantId': item.variantId,
    }).toList();

    final result = await _couponService.validateCoupon(
      code: code,
      orderTotal: orderTotal,
      items: cartItems,
    );

    if (!mounted) return;
    setState(() {
      _isValidatingCoupon = false;
      if (result.valid) {
        _couponApplied = true;
        _discountAmount = result.discountAmount;
        _appliedCouponCode = result.couponCode ?? code.toUpperCase();
        _couponMessage = result.message;
      } else {
        _couponApplied = false;
        _discountAmount = 0.0;
        _appliedCouponCode = null;
        _couponMessage = result.message;
      }
    });
  }

  void _removeCoupon() {
    setState(() {
      _couponController.clear();
      _couponApplied = false;
      _discountAmount = 0.0;
      _appliedCouponCode = null;
      _couponMessage = null;
    });
  }

  Future<void> _fetchAddresses() async {
    setState(() => _isLoadingAddresses = true);
    try {
      final addresses = await _addressService.getAddresses();
      if (!mounted) return;
      setState(() {
        _addresses = addresses;
        // Auto-select default address
        _selectedAddress = addresses.firstWhere(
          (a) => a.isDefault,
          orElse: () => addresses.isNotEmpty
              ? addresses.first
              : Address(
                  id: '',
                  fullName: '',
                  phoneNumber: '',
                  street: '',
                  city: '',
                  state: '',
                  pincode: '',
                  country: '',
                  isDefault: false,
                ),
        );
        if (_selectedAddress!.id.isEmpty) _selectedAddress = null;
        _isLoadingAddresses = false;
      });
      _fetchDeliveryCharge();
    } catch (e) {
      if (!mounted) return;
      setState(() {
        _addresses = [];
        _selectedAddress = null;
        _isLoadingAddresses = false;
      });
      ErrorHandler.showError(context, e);
    }
  }

  Future<void> _placeOrder(
    List<CartItem> itemsToOrder,
    CartProvider cart,
  ) async {
    if (itemsToOrder.isEmpty) return;

    if (_selectedAddress == null || _selectedAddress!.id.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Please select a delivery address')),
      );
      return;
    }

    setState(() => _isProcessing = true);

    final items = itemsToOrder
        .map(
          (item) => {
            "productId": item.product.id, 
            "quantity": item.quantity,
            if (item.variantId != null) "variantId": item.variantId,
          },
        )
        .toList();

    final response = await _orderService.placeOrder(
      items,
      _selectedAddress!.id,
      couponCode: _appliedCouponCode,
    );
    if (!mounted) return;
    setState(() => _isProcessing = false);

    if (response.success) {
      if (widget.buyNowItem == null) {
        cart.clear();
      }
      Navigator.pushReplacement(
        context,
        MaterialPageRoute(builder: (context) => const OrderSuccessPage()),
      );
    } else {
      ErrorHandler.showError(context, response.message);
    }
  }

  @override
  Widget build(BuildContext context) {
    final cart = context.watch<CartProvider>();
    final List<CartItem> itemsToOrder = widget.buyNowItem != null
        ? [widget.buyNowItem!]
        : cart.activeItems;

    double orderSubtotal = 0;

    if (widget.buyNowItem != null) {
      orderSubtotal = widget.buyNowItem!.price * widget.buyNowItem!.quantity;
    } else {
      orderSubtotal = cart.subtotal;
    }

    final double itemsTotal = orderSubtotal;

    return Scaffold(
      backgroundColor: Colors.white,
      appBar: AppBar(
        leading: IconButton(
          icon: const Icon(Icons.arrow_back_ios, size: 20, color: Colors.black),
          onPressed: () => Navigator.pop(context),
        ),
        title: const Text(
          "Checkout",
          style: TextStyle(
            color: Colors.black,
            fontWeight: FontWeight.bold,
            fontSize: 18,
          ),
        ),
        backgroundColor: Colors.white,
        elevation: 0,
        centerTitle: true,
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.symmetric(horizontal: 20),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const SizedBox(height: 20),

            // Delivery Address Section
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                const Row(
                  children: [
                    Icon(Icons.location_on_outlined, size: 20),
                    SizedBox(width: 8),
                    Text(
                      "Delivery Address",
                      style: TextStyle(
                        fontWeight: FontWeight.bold,
                        fontSize: 16,
                      ),
                    ),
                  ],
                ),
                TextButton(
                  onPressed: _showAddressSelector,
                  child: const Text(
                    "Change",
                    style: TextStyle(
                      color: Colors.deepPurple,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                ),
              ],
            ),
            const SizedBox(height: 12),

            _buildAddressSection(),

            const SizedBox(height: 30),

            // Shopping List Section
            const Text(
              "Shopping List",
              style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16),
            ),
            const SizedBox(height: 16),
            ...itemsToOrder.map(
              (item) => _buildShoppingItemCard(context, item),
            ),
            const SizedBox(height: 12),
            _buildPriceSummary(itemsToOrder, orderSubtotal, 0.0),

            const SizedBox(height: 20),

            // Coupon Section
            _buildCouponSection(itemsTotal),

            const SizedBox(height: 40),

            // Place Order Button
            SizedBox(
              width: double.infinity,
              height: 56,
              child: ElevatedButton(
                onPressed: _isProcessing
                    ? null
                    : () => _placeOrder(itemsToOrder, cart),
                style: ElevatedButton.styleFrom(
                  backgroundColor: Colors.deepPurple,
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(16),
                  ),
                  elevation: 0,
                ),
                child: _isProcessing
                    ? const SizedBox(
                        height: 24,
                        width: 24,
                        child: CircularProgressIndicator(
                          color: Colors.white,
                          strokeWidth: 2,
                        ),
                      )
                    : Text(
                        "Confirm Order - ${_formatPrice(itemsTotal + _deliveryCharge - _discountAmount)}",
                        style: const TextStyle(
                          fontSize: 18,
                          fontWeight: FontWeight.bold,
                          color: Colors.white,
                        ),
                      ),
              ),
            ),
            const SizedBox(height: 40),
          ],
        ),
      ),
    );
  }

  Widget _buildAddressSection() {
    if (_isLoadingAddresses) {
      return const Center(
        child: Padding(
          padding: EdgeInsets.all(16),
          child: CircularProgressIndicator(color: Colors.deepPurple),
        ),
      );
    }

    if (_selectedAddress == null) {
      return InkWell(
        onTap: _showAddAddressSheet,
        child: Container(
          width: double.infinity,
          padding: const EdgeInsets.all(16),
          decoration: BoxDecoration(
            border: Border.all(
              color: Colors.deepPurple,
              style: BorderStyle.solid,
            ),
            borderRadius: BorderRadius.circular(12),
            color: Colors.deepPurple.withValues(alpha: 0.03),
          ),
          child: const Row(
            children: [
              Icon(Icons.add_circle_outline, color: Colors.deepPurple),
              SizedBox(width: 12),
              Text(
                "Add a delivery address",
                style: TextStyle(
                  color: Colors.deepPurple,
                  fontWeight: FontWeight.w600,
                ),
              ),
            ],
          ),
        ),
      );
    }

    final addr = _selectedAddress!;
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: Colors.deepPurple.withValues(alpha: 0.4)),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withValues(alpha: 0.04),
            blurRadius: 10,
            offset: const Offset(0, 4),
          ),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Text(
                addr.fullName,
                style: const TextStyle(
                  fontWeight: FontWeight.bold,
                  fontSize: 15,
                ),
              ),
              if (addr.isDefault) ...[
                const SizedBox(width: 8),
                Container(
                  padding: const EdgeInsets.symmetric(
                    horizontal: 6,
                    vertical: 2,
                  ),
                  decoration: BoxDecoration(
                    color: Colors.green.shade50,
                    borderRadius: BorderRadius.circular(4),
                  ),
                  child: Text(
                    "DEFAULT",
                    style: TextStyle(
                      color: Colors.green.shade700,
                      fontSize: 9,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                ),
              ],
            ],
          ),
          const SizedBox(height: 4),
          Text(
            addr.phoneNumber,
            style: TextStyle(color: Colors.grey.shade600, fontSize: 13),
          ),
          const SizedBox(height: 4),
          Text(
            '${addr.street}, ${addr.city} - ${addr.pincode}',
            style: TextStyle(color: Colors.grey.shade600, fontSize: 13),
          ),
          Text(
            '${addr.state}, ${addr.country}',
            style: TextStyle(color: Colors.grey.shade600, fontSize: 13),
          ),
        ],
      ),
    );
  }

  void _showAddressSelector() {
    if (_addresses.isEmpty) {
      _showAddAddressSheet();
      return;
    }

    showModalBottomSheet(
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
                "Select Address",
                style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
              ),
              const SizedBox(height: 16),
              Expanded(
                child: ListView.builder(
                  controller: controller,
                  itemCount: _addresses.length + 1,
                  itemBuilder: (context, index) {
                    if (index == _addresses.length) {
                      return TextButton.icon(
                        onPressed: () {
                          Navigator.pop(context);
                          _showAddAddressSheet();
                        },
                        icon: const Icon(Icons.add, color: Colors.deepPurple),
                        label: const Text(
                          "Add New Address",
                          style: TextStyle(color: Colors.deepPurple),
                        ),
                      );
                    }
                    final addr = _addresses[index];
                    final isSelected = _selectedAddress?.id == addr.id;
                    return GestureDetector(
                      onTap: () {
                        setState(() => _selectedAddress = addr);
                        Navigator.pop(context);
                        _fetchDeliveryCharge();
                      },
                      child: Container(
                        margin: const EdgeInsets.only(bottom: 12),
                        padding: const EdgeInsets.all(16),
                        decoration: BoxDecoration(
                          border: Border.all(
                            color: isSelected
                                ? Colors.deepPurple
                                : Colors.grey.shade200,
                            width: isSelected ? 2 : 1,
                          ),
                          borderRadius: BorderRadius.circular(12),
                        ),
                        child: Row(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Icon(
                              Icons.location_on,
                              color: isSelected
                                  ? Colors.deepPurple
                                  : Colors.grey,
                            ),
                            const SizedBox(width: 12),
                            Expanded(
                              child: Column(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: [
                                  Text(
                                    addr.fullName,
                                    style: const TextStyle(
                                      fontWeight: FontWeight.bold,
                                    ),
                                  ),
                                  Text(
                                    addr.phoneNumber,
                                    style: TextStyle(
                                      color: Colors.grey.shade600,
                                      fontSize: 12,
                                    ),
                                  ),
                                  Text(
                                    '${addr.street}, ${addr.city} - ${addr.pincode}',
                                    style: TextStyle(
                                      color: Colors.grey.shade600,
                                      fontSize: 12,
                                    ),
                                  ),
                                ],
                              ),
                            ),
                            if (isSelected)
                              const Icon(
                                Icons.check_circle,
                                color: Colors.deepPurple,
                              ),
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
  }

  void _showAddAddressSheet() {
    Navigator.push(
      context,
      MaterialPageRoute(
        builder: (context) => AddAddressPage(onSaved: _fetchAddresses),
      ),
    );
  }

  Widget _buildShoppingItemCard(BuildContext context, dynamic item) {
    return Container(
      margin: const EdgeInsets.only(bottom: 20),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(16),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withValues(alpha: 0.03),
            blurRadius: 10,
            offset: const Offset(0, 4),
          ),
        ],
      ),
      child: Padding(
        padding: const EdgeInsets.all(12.0),
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            ClipRRect(
              borderRadius: BorderRadius.circular(12),
              child: item.product.imageUrls.isNotEmpty
                  ? RetryNetworkImage(
                      imageUrl: item.product.imageUrls.first,
                      width: 80,
                      height: 90,
                      fit: BoxFit.cover,
                    )
                  : Container(
                      width: 80,
                      height: 90,
                      color: Colors.grey.shade100,
                      child: const Icon(
                        Icons.inventory_2_outlined,
                        color: Colors.grey,
                      ),
                    ),
            ),
            const SizedBox(width: 16),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    item.product.name,
                    style: const TextStyle(
                      fontWeight: FontWeight.bold,
                      fontSize: 14,
                    ),
                    maxLines: 2,
                    overflow: TextOverflow.ellipsis,
                  ),
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
                  const SizedBox(height: 8),
                  Text(
                    "Qty: ${item.quantity}",
                    style: TextStyle(color: Colors.grey.shade500, fontSize: 13),
                  ),
                  if (item.product.hasPrice) ...[
                    const SizedBox(height: 10),
                    Row(
                      crossAxisAlignment: CrossAxisAlignment.end,
                      children: [
                        Text(
                          _formatPrice(item.price),
                          style: const TextStyle(
                            fontSize: 16,
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
                    const SizedBox(height: 6),
                    Text(
                      'Line total: ${_formatPrice(item.total)}',
                      style: TextStyle(
                        color: Colors.grey.shade700,
                        fontSize: 12,
                        fontWeight: FontWeight.w600,
                      ),
                    ),
                  ],
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildCouponSection(double orderTotal) {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: Colors.grey.shade200),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withValues(alpha: 0.03),
            blurRadius: 10,
            offset: const Offset(0, 4),
          ),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Row(
            children: [
              Icon(
                Icons.local_offer_outlined,
                size: 18,
                color: Colors.deepPurple,
              ),
              SizedBox(width: 8),
              Text(
                'Have a Coupon?',
                style: TextStyle(fontWeight: FontWeight.bold, fontSize: 15),
              ),
            ],
          ),
          const SizedBox(height: 12),
          if (!_couponApplied) ...[
            Row(
              children: [
                Expanded(
                  child: TextField(
                    controller: _couponController,
                    textCapitalization: TextCapitalization.characters,
                    decoration: InputDecoration(
                      hintText: 'Enter coupon code',
                      hintStyle: TextStyle(
                        color: Colors.grey.shade400,
                        fontSize: 14,
                      ),
                      contentPadding: const EdgeInsets.symmetric(
                        horizontal: 14,
                        vertical: 12,
                      ),
                      border: OutlineInputBorder(
                        borderRadius: BorderRadius.circular(10),
                        borderSide: BorderSide(color: Colors.grey.shade300),
                      ),
                      enabledBorder: OutlineInputBorder(
                        borderRadius: BorderRadius.circular(10),
                        borderSide: BorderSide(color: Colors.grey.shade300),
                      ),
                      focusedBorder: OutlineInputBorder(
                        borderRadius: BorderRadius.circular(10),
                        borderSide: const BorderSide(
                          color: Colors.deepPurple,
                          width: 1.5,
                        ),
                      ),
                    ),
                  ),
                ),
                const SizedBox(width: 10),
                SizedBox(
                  height: 48,
                  child: ElevatedButton(
                    onPressed: _isValidatingCoupon
                        ? null
                        : () => _applyCoupon(orderTotal),
                    style: ElevatedButton.styleFrom(
                      backgroundColor: Colors.deepPurple,
                      shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(10),
                      ),
                      elevation: 0,
                    ),
                    child: _isValidatingCoupon
                        ? const SizedBox(
                            width: 20,
                            height: 20,
                            child: CircularProgressIndicator(
                              strokeWidth: 2,
                              color: Colors.white,
                            ),
                          )
                        : const Text(
                            'Apply',
                            style: TextStyle(
                              color: Colors.white,
                              fontWeight: FontWeight.bold,
                            ),
                          ),
                  ),
                ),
              ],
            ),
            if (_couponMessage != null) ...[
              const SizedBox(height: 8),
              Text(
                _couponMessage!,
                style: TextStyle(
                  color: _couponApplied
                      ? Colors.green.shade700
                      : Colors.red.shade600,
                  fontSize: 13,
                  fontWeight: FontWeight.w500,
                ),
              ),
            ],
          ] else ...[
            // Coupon applied banner
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 12),
              decoration: BoxDecoration(
                color: Colors.green.shade50,
                borderRadius: BorderRadius.circular(10),
                border: Border.all(color: Colors.green.shade200),
              ),
              child: Row(
                children: [
                  Icon(
                    Icons.check_circle,
                    color: Colors.green.shade600,
                    size: 20,
                  ),
                  const SizedBox(width: 10),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          _appliedCouponCode ?? '',
                          style: TextStyle(
                            fontWeight: FontWeight.bold,
                            color: Colors.green.shade800,
                            fontSize: 14,
                            letterSpacing: 1,
                          ),
                        ),
                        Text(
                          'You save ${_formatPrice(_discountAmount)}',
                          style: TextStyle(
                            color: Colors.green.shade700,
                            fontSize: 12,
                          ),
                        ),
                      ],
                    ),
                  ),
                  GestureDetector(
                    onTap: _removeCoupon,
                    child: Icon(
                      Icons.close,
                      color: Colors.green.shade700,
                      size: 20,
                    ),
                  ),
                ],
              ),
            ),
          ],
        ],
      ),
    );
  }

  Widget _buildPriceSummary(
    List<CartItem> itemsToOrder,
    double subtotal,
    double autoOfferDiscount,
  ) {
    final int totalQuantity = itemsToOrder.fold(
      0,
      (sum, item) => sum + item.quantity,
    );
    final double itemsTotal = subtotal - autoOfferDiscount;
    final double finalTotal = itemsTotal + _deliveryCharge - _discountAmount;

    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(18),
      decoration: BoxDecoration(
        color: Colors.grey.shade50,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: Colors.grey.shade200),
      ),
      child: Column(
        children: [
          _buildSummaryRow('Subtotal', _formatPrice(subtotal)),
          if (autoOfferDiscount > 0) ...[
            const SizedBox(height: 10),
            _buildSummaryRow(
              'Offer Discount',
              '- ${_formatPrice(autoOfferDiscount)}',
              valueColor: Colors.green,
            ),
          ],
          const SizedBox(height: 10),
          _buildSummaryRow(
            'Delivery Charge',
            _isLoadingDeliveryCharge
                ? 'Calculating...'
                : _formatPrice(_deliveryCharge),
          ),
          if (_couponApplied && _discountAmount > 0) ...[
            const SizedBox(height: 10),
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Row(
                  children: [
                    const Icon(
                      Icons.local_offer,
                      size: 14,
                      color: Colors.green,
                    ),
                    const SizedBox(width: 4),
                    Text(
                      'Coupon (${_appliedCouponCode ?? ''})',
                      style: const TextStyle(
                        fontSize: 14,
                        fontWeight: FontWeight.w500,
                        color: Colors.green,
                      ),
                    ),
                  ],
                ),
                Text(
                  '- ${_formatPrice(_discountAmount)}',
                  style: const TextStyle(
                    fontSize: 15,
                    fontWeight: FontWeight.w700,
                    color: Colors.green,
                  ),
                ),
              ],
            ),
          ],
          const Divider(height: 24),
          _buildSummaryRow(
            'Total Payable',
            _formatPrice(finalTotal.clamp(0, double.infinity)),
            emphasize: true,
          ),
        ],
      ),
    );
  }

  Widget _buildSummaryRow(
    String label,
    String value, {
    bool emphasize = false,
    Color? valueColor,
  }) {
    return Row(
      mainAxisAlignment: MainAxisAlignment.spaceBetween,
      children: [
        Text(
          label,
          style: TextStyle(
            fontSize: emphasize ? 17 : 14,
            fontWeight: emphasize ? FontWeight.w800 : FontWeight.w500,
            color: emphasize ? const Color(0xFF111827) : Colors.grey.shade700,
          ),
        ),
        Text(
          value,
          style: TextStyle(
            fontSize: emphasize ? 20 : 15,
            fontWeight: emphasize ? FontWeight.w900 : FontWeight.w700,
            color:
                valueColor ??
                (emphasize ? Colors.deepPurple : const Color(0xFF111827)),
          ),
        ),
      ],
    );
  }
}

// ---------- Add Address Page ----------

class AddAddressPage extends StatefulWidget {
  final VoidCallback? onSaved;
  final Address? address; // null = add mode, non-null = edit mode

  const AddAddressPage({super.key, this.onSaved, this.address});

  bool get isEditing => address != null;

  @override
  State<AddAddressPage> createState() => _AddAddressPageState();
}

class _AddAddressPageState extends State<AddAddressPage> {
  final _formKey = GlobalKey<FormState>();
  final AddressService _addressService = AddressService();
  bool _isSaving = false;
  late bool _isDefault;

  late final TextEditingController _fullNameCtrl;
  late final TextEditingController _phoneCtrl;
  late final TextEditingController _streetCtrl;
  late final TextEditingController _cityCtrl;
  late final TextEditingController _stateCtrl;
  late final TextEditingController _pincodeCtrl;
  late final TextEditingController _countryCtrl;

  @override
  void initState() {
    super.initState();
    final a = widget.address;
    _fullNameCtrl = TextEditingController(text: a?.fullName ?? '');
    _phoneCtrl = TextEditingController(text: a?.phoneNumber ?? '');
    _streetCtrl = TextEditingController(text: a?.street ?? '');
    _cityCtrl = TextEditingController(text: a?.city ?? '');
    _stateCtrl = TextEditingController(text: a?.state ?? '');
    _pincodeCtrl = TextEditingController(text: a?.pincode ?? '');
    _countryCtrl = TextEditingController(text: a?.country ?? 'India');
    _isDefault = a?.isDefault ?? true;
  }

  @override
  void dispose() {
    _fullNameCtrl.dispose();
    _phoneCtrl.dispose();
    _streetCtrl.dispose();
    _cityCtrl.dispose();
    _stateCtrl.dispose();
    _pincodeCtrl.dispose();
    _countryCtrl.dispose();
    super.dispose();
  }

  Future<void> _save() async {
    if (!_formKey.currentState!.validate()) return;
    setState(() => _isSaving = true);

    try {
      final bool success;
      if (widget.isEditing) {
        success = await _addressService.updateAddress(
          id: widget.address!.id,
          fullName: _fullNameCtrl.text.trim(),
          phoneNumber: _phoneCtrl.text.trim(),
          street: _streetCtrl.text.trim(),
          city: _cityCtrl.text.trim(),
          state: _stateCtrl.text.trim(),
          pincode: _pincodeCtrl.text.trim(),
          country: _countryCtrl.text.trim(),
          isDefault: _isDefault,
        );
      } else {
        success = await _addressService.createAddress(
          fullName: _fullNameCtrl.text.trim(),
          phoneNumber: _phoneCtrl.text.trim(),
          street: _streetCtrl.text.trim(),
          city: _cityCtrl.text.trim(),
          state: _stateCtrl.text.trim(),
          pincode: _pincodeCtrl.text.trim(),
          country: _countryCtrl.text.trim(),
          isDefault: _isDefault,
        );
      }

      if (!mounted) return;
      setState(() => _isSaving = false);

      if (success) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(
              widget.isEditing ? 'Address updated!' : 'Address saved!',
            ),
          ),
        );
        widget.onSaved?.call();
        Navigator.pop(context);
      }
    } catch (e) {
      if (!mounted) return;
      setState(() => _isSaving = false);
      ErrorHandler.showError(context, e);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.white,
      appBar: AppBar(
        title: Text(
          widget.isEditing ? "Edit Address" : "Add Address",
          style: const TextStyle(
            fontWeight: FontWeight.bold,
            color: Colors.black,
          ),
        ),
        centerTitle: true,
        backgroundColor: Colors.white,
        elevation: 0,
        iconTheme: const IconThemeData(color: Colors.black),
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(24),
        child: Form(
          key: _formKey,
          child: Column(
            children: [
              _field(_fullNameCtrl, "Full Name", Icons.person_outline),
              const SizedBox(height: 16),
              _field(
                _phoneCtrl,
                "Phone Number",
                Icons.phone_outlined,
                keyboard: TextInputType.phone,
              ),
              const SizedBox(height: 16),
              _field(_streetCtrl, "Street / Area", Icons.home_outlined),
              const SizedBox(height: 16),
              _field(_cityCtrl, "City", Icons.location_city_outlined),
              const SizedBox(height: 16),
              _field(_stateCtrl, "State", Icons.map_outlined),
              const SizedBox(height: 16),
              _field(
                _pincodeCtrl,
                "Pincode",
                Icons.pin_outlined,
                keyboard: TextInputType.number,
              ),
              const SizedBox(height: 16),
              _field(_countryCtrl, "Country", Icons.flag_outlined),
              const SizedBox(height: 16),
              SwitchListTile(
                contentPadding: EdgeInsets.zero,
                title: const Text(
                  "Set as default address",
                  style: TextStyle(fontWeight: FontWeight.w600),
                ),
                value: _isDefault,
                activeThumbColor: Colors.deepPurple,
                onChanged: (v) => setState(() => _isDefault = v),
              ),
              const SizedBox(height: 32),
              SizedBox(
                width: double.infinity,
                height: 56,
                child: ElevatedButton(
                  onPressed: _isSaving ? null : _save,
                  style: ElevatedButton.styleFrom(
                    backgroundColor: Colors.deepPurple,
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(16),
                    ),
                  ),
                  child: _isSaving
                      ? const CircularProgressIndicator(color: Colors.white)
                      : Text(
                          widget.isEditing ? "Update Address" : "Save Address",
                          style: const TextStyle(
                            fontSize: 16,
                            fontWeight: FontWeight.bold,
                            color: Colors.white,
                          ),
                        ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _field(
    TextEditingController ctrl,
    String label,
    IconData icon, {
    TextInputType keyboard = TextInputType.text,
  }) {
    return TextFormField(
      controller: ctrl,
      keyboardType: keyboard,
      decoration: InputDecoration(
        labelText: label,
        prefixIcon: Icon(icon),
        border: OutlineInputBorder(borderRadius: BorderRadius.circular(12)),
        focusedBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(12),
          borderSide: const BorderSide(color: Colors.deepPurple, width: 2),
        ),
      ),
      validator: (v) =>
          (v == null || v.trim().isEmpty) ? '$label is required' : null,
    );
  }
}
