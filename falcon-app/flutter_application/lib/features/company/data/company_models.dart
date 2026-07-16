class CompanyImage {
  final String? id;
  final String name;
  final String description;
  final String logoUrl;
  final String? iconUrl;
  final String? faviconUrl;
  final String? landingPageImageUrl;

  CompanyImage({
    this.id,
    required this.name,
    required this.description,
    required this.logoUrl,
    this.iconUrl = '',
    this.faviconUrl = '',
    this.landingPageImageUrl = '',
  });

  factory CompanyImage.fromJson(Map<String, dynamic> json) {
    return CompanyImage(
      id: json['id']?.toString(),
      name: json['name'] ?? '',
      description: json['description'] ?? '',
      logoUrl: json['logoUrl'] ?? '',
      iconUrl: json['iconUrl'] ?? '',
      faviconUrl: json['faviconUrl'] ?? '',
      landingPageImageUrl: json['landingPageImageUrl'] ?? '',
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'name': name,
      'description': description,
      'logoUrl': logoUrl,
      'iconUrl': iconUrl,
      'faviconUrl': faviconUrl,
      'landingPageImageUrl': landingPageImageUrl,
    };
  }
}

class CompanySettings {
  final String companyName;
  final String email;
  final String phone;
  final String address;
  final String workingHours;
  final String termsAndConditions;
  final String privacyPolicy;

  CompanySettings({
    required this.companyName,
    required this.email,
    required this.phone,
    required this.address,
    required this.workingHours,
    required this.termsAndConditions,
    required this.privacyPolicy,
  });

  factory CompanySettings.fromJson(Map<String, dynamic> json) {
    return CompanySettings(
      companyName: json['companyName'] ?? '',
      email: json['email'] ?? '',
      phone: json['phone'] ?? '',
      address: json['address'] ?? '',
      workingHours: json['workingHours'] ?? '',
      termsAndConditions: json['termsAndConditions'] ?? '',
      privacyPolicy: json['privacyPolicy'] ?? '',
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'companyName': companyName,
      'email': email,
      'phone': phone,
      'address': address,
      'workingHours': workingHours,
      'termsAndConditions': termsAndConditions,
      'privacyPolicy': privacyPolicy,
    };
  }
}
