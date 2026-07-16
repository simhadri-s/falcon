import 'package:intl/intl.dart';

final NumberFormat _indianCurrencyFormatter = NumberFormat.currency(
  locale: 'en_IN',
  symbol: '₹',
  decimalDigits: 0,
);

String formatIndianPrice(num value) => _indianCurrencyFormatter.format(value);
