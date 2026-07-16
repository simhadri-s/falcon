import 'order_model.dart';

enum ReturnStatus {
  returnRequested,
  returnApproved,
  returnRejected,
  returnPickedUp,
  returnCompleted
}

class ReturnItem {
  final String orderItemId;
  final int quantity;
  final OrderItemProduct product;

  ReturnItem({
    required this.orderItemId,
    required this.quantity,
    required this.product,
  });

  factory ReturnItem.fromJson(Map<String, dynamic> json) {
    return ReturnItem(
      orderItemId: json['orderItemId'] ?? '',
      quantity: json['quantity'] ?? 0,
      product: OrderItemProduct.fromJson(json['productSnapshot'] ?? {}),
    );
  }
}

class ReturnRequest {
  final String id;
  final String orderId;
  final String userId;
  final List<ReturnItem> items;
  final String reason;
  final String comment;
  final ReturnStatus status;
  final String? adminComment;
  final DateTime createdAt;
  final DateTime? updatedAt;

  ReturnRequest({
    required this.id,
    required this.orderId,
    required this.userId,
    required this.items,
    required this.reason,
    required this.comment,
    required this.status,
    this.adminComment,
    required this.createdAt,
    this.updatedAt,
  });

  factory ReturnRequest.fromJson(Map<String, dynamic> json) {
    return ReturnRequest(
      id: json['id'] ?? '',
      orderId: json['orderId'] ?? '',
      userId: json['userId'] ?? '',
      items: (json['items'] as List?)?.map((i) => ReturnItem.fromJson(i)).toList() ?? [],
      reason: json['reason'] ?? '',
      comment: json['comment'] ?? '',
      status: _parseStatus(json['status']),
      adminComment: json['adminComment'],
      createdAt: _parseDate(json['createdAt']),
      updatedAt: json['updatedAt'] != null ? _parseDate(json['updatedAt']) : null,
    );
  }

  static ReturnStatus _parseStatus(String? status) {
    switch (status?.toUpperCase()) {
      case 'RETURN_REQUESTED': return ReturnStatus.returnRequested;
      case 'RETURN_APPROVED': return ReturnStatus.returnApproved;
      case 'RETURN_REJECTED': return ReturnStatus.returnRejected;
      case 'RETURN_PICKED_UP': return ReturnStatus.returnPickedUp;
      case 'RETURN_COMPLETED': return ReturnStatus.returnCompleted;
      default: return ReturnStatus.returnRequested;
    }
  }

  static DateTime _parseDate(String? dateStr) {
    if (dateStr == null) return DateTime.now();
    try {
      return DateTime.parse(dateStr);
    } catch (_) {
      return DateTime.now();
    }
  }

  String get statusDisplay {
    switch (status) {
      case ReturnStatus.returnRequested: return 'Return Requested';
      case ReturnStatus.returnApproved: return 'Approved';
      case ReturnStatus.returnRejected: return 'Rejected';
      case ReturnStatus.returnPickedUp: return 'Picked Up';
      case ReturnStatus.returnCompleted: return 'Completed';
    }
  }
}
