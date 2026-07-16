enum RefundStatus {
  refundPending,
  refundInitiated,
  refundCompleted,
  refundFailed
}

enum RefundMethod {
  originalPayment,
  wallet,
  manual
}

class Refund {
  final String id;
  final String returnRequestId;
  final String orderId;
  final String userId;
  final double amount;
  final RefundStatus status;
  final RefundMethod method;
  final String? transactionId;
  final String? notes;
  final DateTime createdAt;
  final DateTime? updatedAt;

  Refund({
    required this.id,
    required this.returnRequestId,
    required this.orderId,
    required this.userId,
    required this.amount,
    required this.status,
    required this.method,
    this.transactionId,
    this.notes,
    required this.createdAt,
    this.updatedAt,
  });

  factory Refund.fromJson(Map<String, dynamic> json) {
    return Refund(
      id: json['id'] ?? '',
      returnRequestId: json['returnRequestId'] ?? '',
      orderId: json['orderId'] ?? '',
      userId: json['userId'] ?? '',
      amount: (json['amount'] as num?)?.toDouble() ?? 0.0,
      status: _parseStatus(json['status']),
      method: _parseMethod(json['method']),
      transactionId: json['transactionId'],
      notes: json['notes'],
      createdAt: _parseDate(json['createdAt']),
      updatedAt: json['updatedAt'] != null ? _parseDate(json['updatedAt']) : null,
    );
  }

  static RefundStatus _parseStatus(String? status) {
    switch (status?.toUpperCase()) {
      case 'REFUND_PENDING': return RefundStatus.refundPending;
      case 'REFUND_INITIATED': return RefundStatus.refundInitiated;
      case 'REFUND_COMPLETED': return RefundStatus.refundCompleted;
      case 'REFUND_FAILED': return RefundStatus.refundFailed;
      default: return RefundStatus.refundPending;
    }
  }

  static RefundMethod _parseMethod(String? method) {
    switch (method?.toUpperCase()) {
      case 'ORIGINAL_PAYMENT': return RefundMethod.originalPayment;
      case 'WALLET': return RefundMethod.wallet;
      case 'MANUAL': return RefundMethod.manual;
      default: return RefundMethod.manual;
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
}
