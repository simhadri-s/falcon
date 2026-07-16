class Address {
  final String id;
  final String fullName;
  final String phoneNumber;
  final String street;
  final String city;
  final String state;
  final String pincode;
  final String country;
  final bool isDefault;

  Address({
    required this.id,
    required this.fullName,
    required this.phoneNumber,
    required this.street,
    required this.city,
    required this.state,
    required this.pincode,
    required this.country,
    required this.isDefault,
  });

  factory Address.fromJson(Map<String, dynamic> json) {
    return Address(
      id: json['id'] ?? json['_id'] ?? '',
      fullName: json['fullName'] ?? '',
      phoneNumber: json['phoneNumber'] ?? '',
      street: json['street'] ?? '',
      city: json['city'] ?? '',
      state: json['state'] ?? '',
      pincode: json['pincode'] ?? '',
      country: json['country'] ?? '',
      isDefault: json['default'] ?? false,
    );
  }

  String get displayLine1 => fullName;
  String get displayLine2 => '$street, $city - $pincode';
  String get displayLine3 => '$state, $country';
}
