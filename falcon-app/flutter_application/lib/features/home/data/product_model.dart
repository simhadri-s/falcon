class ProductIndustry {
  final String id;
  final String name;
  final String slug;

  ProductIndustry({
    required this.id,
    required this.name,
    required this.slug,
  });

  factory ProductIndustry.fromJson(Map<String, dynamic> json) {
    return ProductIndustry(
      id: json['id'] ?? '',
      name: json['name'] ?? '',
      slug: json['slug'] ?? '',
    );
  }
}

class ProductVariant {
  final String id;
  final String sku;
  final Map<String, String> attributes;
  final double mrp;
  final double sellingPrice;
  final int stockQuantity;

  ProductVariant({
    required this.id,
    required this.sku,
    required this.attributes,
    required this.mrp,
    required this.sellingPrice,
    required this.stockQuantity,
  });

  factory ProductVariant.fromJson(Map<String, dynamic> json) {
    return ProductVariant(
      id: json['id'] ?? '',
      sku: json['sku'] ?? '',
      attributes: Map<String, String>.from(json['attributes'] ?? {}),
      mrp: Product._asDouble(json['mrp']),
      sellingPrice: Product._asDouble(json['sellingPrice']),
      stockQuantity: json['stockQuantity'] ?? 0,
    );
  }
}

class Product {
  final String id;
  final String productCode;
  final String name;
  final String category;
  final String categoryId;
  final String categoryName;
  final String subCategoryId;
  final String subCategoryName;
  final String description;
  final List<String> imageUrls;
  final bool featured;
  final double price;
  final double mrp;
  final double sellingPrice;
  final Map<String, String> specifications;
  final List<ProductIndustry> industries;
  final bool manageStock;
  final int stockQuantity;
  final double averageRating;
  final int reviewCount;
  final bool isOnExpiryOffer;
  final double originalSellingPrice;
  final bool hasVariants;
  final List<ProductVariant> variants;

  Product({
    required this.id,
    this.productCode = '',
    required this.name,
    required this.category,
    this.categoryId = '',
    this.categoryName = '',
    this.subCategoryId = '',
    this.subCategoryName = '',
    required this.description,
    required this.imageUrls,
    required this.featured,
    this.price = 0.0,
    this.mrp = 0.0,
    this.sellingPrice = 0.0,
    this.specifications = const {},
    this.industries = const [],
    this.manageStock = false,
    this.stockQuantity = 0,
    this.averageRating = 0.0,
    this.reviewCount = 0,
    this.isOnExpiryOffer = false,
    this.originalSellingPrice = 0.0,
    this.hasVariants = false,
    this.variants = const [],
  });

  bool get hasPrice => price > 0;
  bool get hasMrp => mrp > 0;
  bool get hasSellingPrice => sellingPrice > 0;
  bool get hasOffer => isOnExpiryOffer;
  bool get hasDiscount => hasOffer || (hasSellingPrice && hasMrp && mrp > sellingPrice);
  bool get isOutOfStock => manageStock && stockQuantity <= 0;

  String get offerName => isOnExpiryOffer ? 'Special Expiry Offer' : 'Limited Deal';
  double get offerPrice => sellingPrice;
  double get originalPrice => isOnExpiryOffer && originalSellingPrice > 0 ? originalSellingPrice : price;

  factory Product.fromJson(Map<String, dynamic> json) {
    final double parsedSellingPrice = _asDouble(json['sellingPrice']);
    final double parsedMrp = _asDouble(json['mrp']);
    final double legacyPrice = _asDouble(json['price']);

    return Product(
      id: json['id'] ?? json['_id'] ?? '',
      productCode: json['productCode'] ?? '',
      name: json['name'] ?? '',
      category: json['category'] ?? '',
      categoryId: json['categoryId'] ?? '',
      categoryName: json['categoryName'] ?? '',
      subCategoryId: json['subCategoryId'] ?? '',
      subCategoryName: json['subCategoryName'] ?? '',
      description: json['description'] ?? '',
      imageUrls: List<String>.from(json['imageUrls'] ?? []),
      featured: json['featured'] ?? json['isFeatured'] ?? false,
      price: parsedSellingPrice > 0
          ? parsedSellingPrice
          : (parsedMrp > 0 ? parsedMrp : legacyPrice),
      mrp: parsedMrp,
      sellingPrice: parsedSellingPrice,
      specifications: {
        for (var spec in (json['specs'] as List? ?? []))
          if (spec is Map) spec['key']?.toString() ?? '': spec['value']?.toString() ?? ''
      },
      industries: (json['industries'] as List? ?? [])
          .map((item) => ProductIndustry.fromJson(item as Map<String, dynamic>))
          .toList(),
      manageStock: json['manageStock'] ?? false,
      stockQuantity: json['stockQuantity'] ?? 0,
      averageRating: _asDouble(json['averageRating']),
      reviewCount: json['reviewCount'] ?? 0,
      isOnExpiryOffer: json['isOnExpiryOffer'] ?? false,
      originalSellingPrice: _asDouble(json['originalSellingPrice']),
      hasVariants: json['hasVariants'] ?? false,
      variants: (json['variants'] as List? ?? [])
          .map((v) => ProductVariant.fromJson(v as Map<String, dynamic>))
          .toList(),
    );
  }

  static double _asDouble(dynamic value) {
    if (value is num) {
      return value.toDouble();
    }
    return double.tryParse(value?.toString() ?? '') ?? 0.0;
  }
}
