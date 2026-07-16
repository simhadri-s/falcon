package com.laserxprts.falcon.service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.laserxprts.falcon.dto.response.ProductResponse;
import com.laserxprts.falcon.exception.ApiException;
import com.laserxprts.falcon.exception.DuplicateResourceException;
import com.laserxprts.falcon.exception.ResourceNotFoundException;
import com.laserxprts.falcon.model.Industry;
import com.laserxprts.falcon.model.Product;
import com.laserxprts.falcon.model.Product.IndustryRef;
import com.laserxprts.falcon.model.ProductCategory;
import com.laserxprts.falcon.model.SubCategory;
import com.laserxprts.falcon.repository.IndustryRepository;
import com.laserxprts.falcon.repository.ProductCategoryRepository;
import com.laserxprts.falcon.repository.ProductRepository;
import com.laserxprts.falcon.repository.SubCategoryRepository;
import com.laserxprts.falcon.repository.CartRepository;
import com.laserxprts.falcon.repository.WishlistRepository;
import com.laserxprts.falcon.repository.ReviewRepository;
import com.laserxprts.falcon.model.Cart;
import com.laserxprts.falcon.model.Wishlist;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final FileUploadService fileUploadService;
    private final ProductCategoryRepository productCategoryRepository;
    private final SubCategoryRepository subCategoryRepository;
    private final IndustryRepository industryRepository;
    private final ProductCategoryService productCategoryService;
    private final SubCategoryService subCategoryService;
    private final CartRepository cartRepository;
    private final WishlistRepository wishlistRepository;
    private final ReviewRepository reviewRepository;

    public ProductService(
            ProductRepository productRepository,
            FileUploadService fileUploadService,
            ProductCategoryRepository productCategoryRepository,
            SubCategoryRepository subCategoryRepository,
            IndustryRepository industryRepository,
            ProductCategoryService productCategoryService,
            SubCategoryService subCategoryService,
            CartRepository cartRepository,
            WishlistRepository wishlistRepository,
            ReviewRepository reviewRepository) {
        this.productRepository = productRepository;
        this.fileUploadService = fileUploadService;
        this.productCategoryRepository = productCategoryRepository;
        this.subCategoryRepository = subCategoryRepository;
        this.industryRepository = industryRepository;
        this.productCategoryService = productCategoryService;
        this.subCategoryService = subCategoryService;
        this.cartRepository = cartRepository;
        this.wishlistRepository = wishlistRepository;
        this.reviewRepository = reviewRepository;
    }

    private List<IndustryRef> resolveIndustries(List<String> slugs) {
        if (slugs == null || slugs.isEmpty()) return new ArrayList<>();

        List<String> trimmedSlugs = slugs.stream().map(String::trim).collect(java.util.stream.Collectors.toList());
        List<Industry> industries = industryRepository.findBySlugIn(trimmedSlugs);
        Map<String, Industry> industryMap = industries.stream().collect(java.util.stream.Collectors.toMap(Industry::getSlug, i -> i));

        List<IndustryRef> refs = new ArrayList<>();
        for (String slug : trimmedSlugs) {
            Industry industry = industryMap.get(slug);
            if (industry == null) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid industry slug: \"" + slug + "\"");
            }
            IndustryRef ref = new IndustryRef();
            ref.setId(industry.getId());
            ref.setName(industry.getName());
            ref.setSlug(industry.getSlug());
            refs.add(ref);
        }
        return refs;
    }

    private void validatePricePair(Double mrp, Double sellingPrice) {
        if (mrp != null && mrp < 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "MRP cannot be negative");
        }
        if (sellingPrice != null && sellingPrice < 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Selling price cannot be negative");
        }
        if (mrp != null && sellingPrice != null && sellingPrice > mrp) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Selling price cannot be greater than MRP");
        }
    }

    private Double parseOptionalPrice(CSVRecord csvRecord, String header) {
        if (!csvRecord.isMapped(header)) {
            return null;
        }

        String rawValue = csvRecord.get(header);
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }

        try {
            return Double.valueOf(rawValue.trim());
        } catch (NumberFormatException exception) {
            throw new RuntimeException("Invalid " + header + " value: " + rawValue);
        }
    }
    

    public Page<ProductResponse> getAllProducts(String category, String subCategory, String keyword, int page, int limit, String sortBy, String sortDirection, boolean isAdmin) {
        String sortField = (sortBy != null && !sortBy.isEmpty()) ? sortBy : "createdAt";
        if (sortField.equals("featured")) sortField = "isFeatured";
        if (sortField.equals("sellingPrice")) sortField = "effectivePrice";
        
        Sort.Direction direction = Sort.Direction.DESC;
        if (sortDirection != null && sortDirection.equalsIgnoreCase("asc")) {
            direction = Sort.Direction.ASC;
        }
        Sort sort = Sort.by(direction, sortField);
        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), limit, sort);

        // Resolve identifiers if they are not IDs
        String resolvedSubCategoryId = subCategory;
        if (subCategory != null && !subCategory.isEmpty() && !isValidObjectId(subCategory)) {
            resolvedSubCategoryId = subCategoryService.getSubCategoryByIdentifier(subCategory)
                .map(sc -> sc.getId())
                .orElse(subCategory);
        }

        String resolvedCategoryId = category;
        if (category != null && !category.isEmpty() && !isValidObjectId(category)) {
            resolvedCategoryId = productCategoryService.getCategoryByIdentifier(category)
                .map(cat -> cat.getId())
                .orElse(category);
        }

        if (resolvedSubCategoryId != null && !resolvedSubCategoryId.isEmpty() && keyword != null && !keyword.isEmpty()) {
            return enrichPage(productRepository.filterBySubCategoryAndKeyword(resolvedSubCategoryId, keyword, pageable, isAdmin));
        }

        if (resolvedSubCategoryId != null && !resolvedSubCategoryId.isEmpty()) {
            Page<Product> productPage = isAdmin
                ? productRepository.findBySubCategoryId(resolvedSubCategoryId, pageable)
                : productRepository.findBySubCategoryIdAndPublishedTrue(resolvedSubCategoryId, pageable);
            return enrichPage(productPage);
        }

        if (resolvedCategoryId != null && !resolvedCategoryId.isEmpty() && keyword != null && !keyword.isEmpty()) {
            return enrichPage(productRepository.filterByCategoryAndKeyword(resolvedCategoryId, keyword, pageable, isAdmin));
        }

        if (resolvedCategoryId != null && !resolvedCategoryId.isEmpty()) {
            Page<Product> productPage;
            productPage = isAdmin
                ? productRepository.findByCategoryId(resolvedCategoryId, pageable)
                : productRepository.findByCategoryIdAndPublishedTrue(resolvedCategoryId, pageable);
            
            if (productPage.isEmpty()) {
                productPage = isAdmin
                    ? productRepository.findByCategoryIgnoreCase(resolvedCategoryId, pageable)
                    : productRepository.findByCategoryIgnoreCaseAndPublishedTrue(resolvedCategoryId, pageable);
            }
            return enrichPage(productPage);
        }

        if (keyword != null && !keyword.isEmpty()) {
            return enrichPage(productRepository.searchByKeyword(keyword, pageable, isAdmin));
        }

        return isAdmin
            ? enrichPage(productRepository.findAll(pageable))
            : enrichPage(productRepository.findByPublishedTrue(pageable));
    }

    public Page<ProductResponse> enrichPage(Page<Product> page) {
        java.util.Set<String> subCategoryIds = page.getContent().stream()
            .map(Product::getSubCategoryId)
            .filter(id -> id != null && !id.isEmpty())
            .collect(java.util.stream.Collectors.toSet());
        
        Map<String, String> subCategoryNames = new HashMap<>();
        if (!subCategoryIds.isEmpty()) {
            subCategoryRepository.findAllById(subCategoryIds).forEach(sc -> 
                subCategoryNames.put(sc.getId(), sc.getName())
            );
        }

        return page.map(product -> {
            ProductResponse.ProductResponseBuilder builder = ProductResponse.from(product).toBuilder();
            builder.categoryName(product.getCategory());
            if (product.getSubCategoryId() != null) {
                builder.subCategoryName(subCategoryNames.get(product.getSubCategoryId()));
            }
            return builder.build();
        });
    }

    public List<ProductResponse> enrichList(List<Product> products) {
        java.util.Set<String> subCategoryIds = products.stream()
            .map(Product::getSubCategoryId)
            .filter(id -> id != null && !id.isEmpty())
            .collect(java.util.stream.Collectors.toSet());
        
        Map<String, String> subCategoryNames = new HashMap<>();
        if (!subCategoryIds.isEmpty()) {
            subCategoryRepository.findAllById(subCategoryIds).forEach(sc -> 
                subCategoryNames.put(sc.getId(), sc.getName())
            );
        }

        return products.stream().map(product -> {
            ProductResponse.ProductResponseBuilder builder = ProductResponse.from(product).toBuilder();
            builder.categoryName(product.getCategory());
            if (product.getSubCategoryId() != null) {
                builder.subCategoryName(subCategoryNames.get(product.getSubCategoryId()));
            }
            return builder.build();
        }).collect(java.util.stream.Collectors.toList());
    }

    public ProductResponse enrichResponse(Product product) {
        return enrich(product);
    }

    private ProductResponse enrich(Product product) {
        ProductResponse.ProductResponseBuilder builder = ProductResponse.from(product).toBuilder();
        builder.categoryName(product.getCategory());
        
        if (product.getSubCategoryId() != null) {
            subCategoryRepository.findById(product.getSubCategoryId())
                .ifPresent(sub -> builder.subCategoryName(sub.getName()));
        }

        return builder.build();
    }

    public Page<ProductResponse> getFeaturedProducts(int page, int limit, boolean isAdmin) {
        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), limit);

        return isAdmin
            ? productRepository.findByIsFeaturedTrue(pageable).map(ProductResponse::from)
            : productRepository.findByIsFeaturedTrueAndPublishedTrue(pageable).map(ProductResponse::from);
    }

    public ProductResponse getProductBySlug(String slug) {
        if (slug == null || slug.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Product slug is required");
        }
        Product product = productRepository.findBySlug(slug)
            .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        return enrich(product);
    }

    public ProductResponse getProductById(String id) {
        if (id == null || id.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Product ID is required");
        }
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        return ProductResponse.from(product);
    }

    public Page<ProductResponse> getProductByIndustry(String slug, int page, int limit, String sortBy, String sortDirection) {
        if (slug == null || slug.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Industry slug is required");
        }
        String sortField = (sortBy != null && !sortBy.isEmpty()) ? sortBy : "createdAt";
        if (sortField.equals("featured")) sortField = "isFeatured";
        if (sortField.equals("sellingPrice")) sortField = "effectivePrice";
        
        Sort.Direction direction = Sort.Direction.DESC;
        if (sortDirection != null && sortDirection.equalsIgnoreCase("asc")) {
            direction = Sort.Direction.ASC;
        }
        Sort sort = Sort.by(direction, sortField);
        Pageable pageable = PageRequest.of(Math.max(0, page - 1), limit, sort);
        return enrichPage(productRepository.findByIndustriesSlug(slug, pageable));
    }

    public ProductResponse createProduct(Product product, List<MultipartFile> images) {
        if (product.getProductCode() == null || product.getProductCode().isBlank() || productRepository.existsByProductCode(product.getProductCode().trim())) {
            product.setProductCode(generateUniqueProductCode());
        }
        if (product.getName() == null || product.getName().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Product name is required");
        }
        
        // Handle Category and Migration
        if (product.getCategoryId() != null && !product.getCategoryId().isBlank()) {
            if (!productCategoryRepository.existsById(product.getCategoryId())) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid Product Category.");
            }
            // Populate category name for backwards compatibility if needed
            productCategoryRepository.findById(product.getCategoryId())
                .ifPresent(cat -> product.setCategory(cat.getName()));
        } else if (product.getCategory() != null) {
            // If only name provided, try to find ID
            productCategoryRepository.findByNameIgnoreCase(product.getCategory())
                .ifPresent(cat -> product.setCategoryId(cat.getId()));
            
            if (product.getCategoryId() == null) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid or missing Product Category.");
            }
        } else {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Product Category is required.");
        }

        // Validate Sub-Category if provided
        if (product.getSubCategoryId() != null && !product.getSubCategoryId().isBlank() && !subCategoryRepository.existsById(product.getSubCategoryId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid Sub-Category.");
        }
        validatePricePair(product.getMrp(), product.getSellingPrice());

        String slug = generateSlug(product.getName());
        if (productRepository.existsBySlug(slug)) {
            throw new DuplicateResourceException("Product already exists");
        }

        product.setSlug(slug);
        product.setCreatedAt(LocalDateTime.now());

        // CHANGED: resolve industry slugs sent from the frontend into embedded snapshots
        // Frontend should send a list of slugs e.g. ["laser", "cutting"]
        List<String> slugsFromRequest = product.getIndustrySlugs(); // see note below
        product.setIndustries(resolveIndustries(slugsFromRequest));

        if (images != null && !images.isEmpty()) {
            product.setImageUrls(fileUploadService.uploadMultipleImages(images));
        } else {
            product.setImageUrls(new ArrayList<>());
        }

        if (product.isHasVariants() && product.getVariants() != null && !product.getVariants().isEmpty()) {
            Double lowestPrice = product.getVariants().stream()
                .map(v -> v.getSellingPrice() != null && v.getSellingPrice() > 0 ? v.getSellingPrice() : (v.getMrp() != null ? v.getMrp() : 0.0))
                .min(Double::compareTo)
                .orElse(0.0);
            product.setEffectivePrice(lowestPrice);
        } else {
            product.setEffectivePrice(product.getSellingPrice() != null && product.getSellingPrice() > 0 ? product.getSellingPrice() : product.getMrp());
        }
        
        reevaluateExpiryOffer(product);

        return enrich(productRepository.save(product));
    }

    public ProductResponse updateProduct(
        String id,
        Product updatedProduct,
        List<MultipartFile> images,
        boolean isMrpProvided,
        boolean isSellingPriceProvided
    ) {
        if (id == null || id.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid product ID");
        }

        Product existingProduct = productRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        if (updatedProduct.getProductCode() != null && !updatedProduct.getProductCode().isBlank()) {
            if (!updatedProduct.getProductCode().equals(existingProduct.getProductCode()) && 
                productRepository.existsByProductCode(updatedProduct.getProductCode())) {
                throw new DuplicateResourceException("Product Code already exists");
            }
            existingProduct.setProductCode(updatedProduct.getProductCode());
        } else if (existingProduct.getProductCode() == null || existingProduct.getProductCode().isBlank()) {
            existingProduct.setProductCode(generateUniqueProductCode());
        }

        if (updatedProduct.getName() != null && !updatedProduct.getName().isBlank()) {
            String newSlug = generateSlug(updatedProduct.getName());
            if (!newSlug.equals(existingProduct.getSlug()) && productRepository.existsBySlug(newSlug)) {
                throw new DuplicateResourceException("Product or slug already exists");
            }
            existingProduct.setName(updatedProduct.getName());
            existingProduct.setSlug(newSlug);
        }

        if (updatedProduct.getDescription() != null && !updatedProduct.getDescription().isBlank()) {
            existingProduct.setDescription(updatedProduct.getDescription());
        }

        if (updatedProduct.getCategoryId() != null && !updatedProduct.getCategoryId().isBlank()) {
            if (!productCategoryRepository.existsById(updatedProduct.getCategoryId())) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid Product Category.");
            }
            existingProduct.setCategoryId(updatedProduct.getCategoryId());
            productCategoryRepository.findById(updatedProduct.getCategoryId())
                .ifPresent(cat -> existingProduct.setCategory(cat.getName()));
        } else if (updatedProduct.getCategory() != null && !updatedProduct.getCategory().isBlank()) {
            if (!productCategoryRepository.existsByNameIgnoreCase(updatedProduct.getCategory())) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid Product Category.");
            }
            existingProduct.setCategory(updatedProduct.getCategory());
            productCategoryRepository.findByNameIgnoreCase(updatedProduct.getCategory())
                .ifPresent(cat -> existingProduct.setCategoryId(cat.getId()));
        }

        if (updatedProduct.getSubCategoryId() != null && !updatedProduct.getSubCategoryId().isBlank()) {
            if (!subCategoryRepository.existsById(updatedProduct.getSubCategoryId())) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid Sub-Category.");
            }
            existingProduct.setSubCategoryId(updatedProduct.getSubCategoryId());
        }

        if (updatedProduct.getSpecs() != null && !updatedProduct.getSpecs().isEmpty()) {
            existingProduct.setSpecs(updatedProduct.getSpecs());
        }

        if (updatedProduct.getVariants() != null) {
            existingProduct.setVariants(updatedProduct.getVariants());
        }
        existingProduct.setHasVariants(updatedProduct.isHasVariants());

        if (isMrpProvided || isSellingPriceProvided) {
            Double nextMrp = isMrpProvided ? updatedProduct.getMrp() : existingProduct.getMrp();
            Double nextSellingPrice = isSellingPriceProvided ? updatedProduct.getSellingPrice() : existingProduct.getSellingPrice();
            validatePricePair(nextMrp, nextSellingPrice);

            if (isMrpProvided) {
                existingProduct.setMrp(updatedProduct.getMrp());
            }
            if (isSellingPriceProvided) {
                existingProduct.setSellingPrice(updatedProduct.getSellingPrice());
            }
        }

        if (existingProduct.isHasVariants() && existingProduct.getVariants() != null && !existingProduct.getVariants().isEmpty()) {
            Double lowestPrice = existingProduct.getVariants().stream()
                .map(v -> v.getSellingPrice() != null && v.getSellingPrice() > 0 ? v.getSellingPrice() : (v.getMrp() != null ? v.getMrp() : 0.0))
                .min(Double::compareTo)
                .orElse(0.0);
            existingProduct.setEffectivePrice(lowestPrice);
        } else {
            Double nextMrp = existingProduct.getMrp();
            Double nextSellingPrice = existingProduct.getSellingPrice();
            existingProduct.setEffectivePrice(nextSellingPrice != null && nextSellingPrice > 0 ? nextSellingPrice : nextMrp);
        }

        if (updatedProduct.getIndustrySlugs() != null && !updatedProduct.getIndustrySlugs().isEmpty()) {
            
            existingProduct.setIndustries(resolveIndustries(updatedProduct.getIndustrySlugs()));
        }

        if (images != null && !images.isEmpty()) {
            fileUploadService.deleteFiles(existingProduct.getImageUrls());
            existingProduct.setImageUrls(fileUploadService.uploadMultipleImages(images));
        }

        existingProduct.setPublished(updatedProduct.isPublished());
        existingProduct.setFeatured(updatedProduct.isFeatured());
        existingProduct.setStockQuantity(updatedProduct.getStockQuantity());
        existingProduct.setManageStock(updatedProduct.isManageStock());
        existingProduct.setExpiryDate(updatedProduct.getExpiryDate());
        
        existingProduct.setAutoOfferOnExpiry(updatedProduct.isAutoOfferOnExpiry());
        if (updatedProduct.getExpiryThresholdDays() != null) {
            existingProduct.setExpiryThresholdDays(updatedProduct.getExpiryThresholdDays());
        }
        if (updatedProduct.getExpiryDiscountPercent() != null) {
            existingProduct.setExpiryDiscountPercent(updatedProduct.getExpiryDiscountPercent());
        }
        
        reevaluateExpiryOffer(existingProduct);

        return enrich(productRepository.save(existingProduct));
    }

    public void deleteProduct(String id) {
        if (id == null || id.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid product ID");
        }
        Product existingProduct = productRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        reviewRepository.deleteByProductId(id);

        List<Cart> affectedCarts = cartRepository.findByItemsProductId(id);
        for (Cart cart : affectedCarts) {
            boolean removed = cart.getItems().removeIf(item -> item.getProduct() != null && item.getProduct().getId().equals(id));
            if (removed) {
                cartRepository.save(cart);
            }
        }

        List<Wishlist> affectedWishlists = wishlistRepository.findByProductsId(id);
        for (Wishlist wishlist : affectedWishlists) {
            boolean removed = wishlist.getProducts().removeIf(p -> p != null && p.getId().equals(id));
            if (removed) {
                wishlistRepository.save(wishlist);
            }
        }

        fileUploadService.deleteFiles(existingProduct.getImageUrls());
        productRepository.deleteById(id);
    }

    public ProductResponse togglePublishStatus(String id) {
        Product existingProduct = productRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        existingProduct.setPublished(!existingProduct.isPublished());
        return enrich(productRepository.save(existingProduct));
    }

    public ProductResponse toggleFeaturedStatus(String id) {
        Product existingProduct = productRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        existingProduct.setFeatured(!existingProduct.isFeatured());
        return enrich(productRepository.save(existingProduct));
    }

    private String generateSlug(String title) {
        return title.toLowerCase()
            .replaceAll("[^a-z0-9\\s]", "")
            .trim()
            .replaceAll("\\s+", "-");
    }

    private String generateUniqueProductCode() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        java.util.Random rnd = new java.util.Random();
        while (true) {
            StringBuilder sb = new StringBuilder(6);
            for (int i = 0; i < 6; i++) {
                sb.append(chars.charAt(rnd.nextInt(chars.length())));
            }
            String code = sb.toString();
            if (!productRepository.existsByProductCode(code)) {
                return code;
            }
        }
    }

    public Map<String, Object> bulkUploadProducts(MultipartFile file) {
        if (file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Please upload a valid CSV file");
        }

        int successCount = 0;
        List<String> errors = new ArrayList<>();

        // In-memory caches for batch processing
        Map<String, ProductCategory> categoryCache = new HashMap<>();
        productCategoryRepository.findAll().forEach(c -> categoryCache.put(c.getName().toLowerCase(), c));

        Map<String, SubCategory> subCategoryCache = new HashMap<>();
        subCategoryRepository.findAll().forEach(sc -> subCategoryCache.put(sc.getName().toLowerCase(), sc));

        Map<String, Industry> industryCache = new HashMap<>();
        industryRepository.findAll().forEach(i -> industryCache.put(i.getSlug(), i));

        java.util.Set<String> localSlugs = new java.util.HashSet<>();
        java.util.Set<String> localCodes = new java.util.HashSet<>();
        
        List<Product> batchToSave = new ArrayList<>();
        final int BATCH_SIZE = 500;

        try (BufferedReader fileReader = new BufferedReader(new InputStreamReader(file.getInputStream(), "UTF-8"));
             CSVParser csvParser = new CSVParser(fileReader,
                 CSVFormat.DEFAULT.withFirstRecordAsHeader().withIgnoreHeaderCase().withTrim())) {

            for (CSVRecord csvRecord : csvParser.getRecords()) {
                long rowNumber = csvRecord.getRecordNumber();
                try {
                    String name = csvRecord.get("Name");
                    if (name == null || name.isBlank()) throw new RuntimeException("Name is empty");
                    String slug = generateSlug(name);
                    
                    if (localSlugs.contains(slug) || productRepository.existsBySlug(slug)) {
                        throw new RuntimeException("Product name already exists (duplicate slug)");
                    }
                    localSlugs.add(slug);

                    String productCodeRaw = csvRecord.isMapped("ProductCode") ? csvRecord.get("ProductCode") : null;
                    String productCode;
                    if (productCodeRaw == null || productCodeRaw.isBlank()) {
                        productCode = generateUniqueProductCodeWithCache(localCodes);
                    } else {
                        productCode = productCodeRaw.trim();
                        if (localCodes.contains(productCode) || productRepository.existsByProductCode(productCode)) {
                            productCode = generateUniqueProductCodeWithCache(localCodes);
                        }
                    }
                    localCodes.add(productCode);

                    String categoryName = csvRecord.get("Category");
                    String subCategoryName = csvRecord.isMapped("SubCategory") ? csvRecord.get("SubCategory") : null;

                    if (categoryName == null || categoryName.isBlank()) {
                        throw new RuntimeException("Category is empty");
                    }
                    
                    String catKey = categoryName.trim().toLowerCase();
                    ProductCategory category = categoryCache.get(catKey);
                    if (category == null) {
                        ProductCategory newCat = new ProductCategory();
                        newCat.setName(categoryName.trim());
                        newCat.setSlug(generateSlug(categoryName.trim()));
                        category = productCategoryRepository.save(newCat);
                        categoryCache.put(catKey, category);
                    }

                    String subCategoryId = null;
                    if (subCategoryName != null && !subCategoryName.isBlank()) {
                        String subName = subCategoryName.trim();
                        String subKey = subName.toLowerCase();
                        SubCategory subCategory = subCategoryCache.get(subKey);
                        if (subCategory == null) {
                            SubCategory newSub = new SubCategory();
                            newSub.setName(subName);
                            newSub.setSlug(generateSlug(subName));
                            newSub.setCategoryId(category.getId());
                            subCategory = subCategoryRepository.save(newSub);
                            subCategoryCache.put(subKey, subCategory);
                        }
                        subCategoryId = subCategory.getId();
                    }

                    Product product = new Product();
                    product.setName(name);
                    product.setProductCode(productCode);
                    product.setCategory(category.getName());
                    product.setCategoryId(category.getId());
                    product.setSubCategoryId(subCategoryId);
                    product.setSlug(slug);
                    product.setDescription(csvRecord.isMapped("Description") ? csvRecord.get("Description") : "");
                    product.setMrp(parseOptionalPrice(csvRecord, "MRP"));
                    product.setSellingPrice(parseOptionalPrice(csvRecord, "SellingPrice"));
                    validatePricePair(product.getMrp(), product.getSellingPrice());
                    product.setEffectivePrice(product.getSellingPrice() != null && product.getSellingPrice() > 0 ? product.getSellingPrice() : product.getMrp());
                    product.setFeatured(csvRecord.isMapped("Featured") && Boolean.parseBoolean(csvRecord.get("Featured")));
                    product.setPublished(csvRecord.isMapped("Published") && Boolean.parseBoolean(csvRecord.get("Published")));

                    if (csvRecord.isMapped("Quantity") && csvRecord.get("Quantity") != null && !csvRecord.get("Quantity").isBlank()) {
                        try {
                            int qty = Integer.parseInt(csvRecord.get("Quantity").trim());
                            product.setStockQuantity(qty);
                            product.setManageStock(true);
                        } catch (NumberFormatException e) {
                            product.setStockQuantity(0);
                            product.setManageStock(false);
                        }
                    } else {
                        product.setStockQuantity(0);
                        product.setManageStock(false);
                    }

                    if (csvRecord.isMapped("Industries") && !csvRecord.get("Industries").isBlank()) {
                        List<String> rawNames = Arrays.asList(csvRecord.get("Industries").split("\\s*;\\s*"));
                        product.setIndustries(resolveOrCreateIndustriesWithCache(rawNames, industryCache));
                    } else {
                        product.setIndustries(new ArrayList<>());
                    }

                    product.setImageUrls(new ArrayList<>());
                    product.setCreatedAt(LocalDateTime.now());

                    batchToSave.add(product);
                    successCount++;

                    if (batchToSave.size() >= BATCH_SIZE) {
                        productRepository.saveAll(batchToSave);
                        batchToSave.clear();
                    }

                } catch (Exception e) {
                    errors.add("Row " + rowNumber + " Failed: " + e.getMessage());
                }
            }

            if (!batchToSave.isEmpty()) {
                productRepository.saveAll(batchToSave);
                batchToSave.clear();
            }

        } catch (Exception e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Failed to parse CSV file: " + e.getMessage());
        }

        Map<String, Object> response = new HashMap<>();
        response.put("successfulUploads", successCount);

        response.put("failedUploads", errors.size());
        response.put("errors", errors);
        return response;
    }

    private String generateUniqueProductCodeWithCache(java.util.Set<String> localCodes) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        
        java.util.Random rnd = new java.util.Random();
        while (true) {
            StringBuilder sb = new StringBuilder(6);
            for (int i = 0; i < 6; i++) {
                sb.append(chars.charAt(rnd.nextInt(chars.length())));
            }
            String code = sb.toString();
            if (!localCodes.contains(code) && !productRepository.existsByProductCode(code)) {
                return code;
            }
        }
    }

    private List<IndustryRef> resolveOrCreateIndustriesWithCache(List<String> names, Map<String, Industry> industryCache) {
        if (names == null || names.isEmpty()) return new ArrayList<>();

        List<String> trimmedNames = names.stream().map(String::trim).collect(java.util.stream.Collectors.toList());
        List<IndustryRef> refs = new ArrayList<>();
        
        for (String trimmed : trimmedNames) {
            String slug = trimmed.toLowerCase().replaceAll("[^a-z0-9]+", "-");
            Industry industry = industryCache.get(slug);
            if (industry == null) {
                java.util.Optional<Industry> existing = industryRepository.findBySlug(slug);
                if (existing.isPresent()) {
                    industry = existing.get();
                } else {
                    Industry newIndustry = new Industry();
                    newIndustry.setName(trimmed);
                    newIndustry.setSlug(slug);
                    industry = industryRepository.save(newIndustry);
                }
                industryCache.put(slug, industry);
            }
            
            IndustryRef ref = new IndustryRef();
            ref.setId(industry.getId());
            ref.setName(industry.getName());
            ref.setSlug(industry.getSlug());
            refs.add(ref);
        }
        return refs;
    }

    // Kept for backward compatibility with older un-batched methods
    private List<IndustryRef> resolveOrCreateIndustries(List<String> names) {
        return resolveOrCreateIndustriesWithCache(names, new HashMap<>());
    }

    public ProductResponse updateStockAndPrice(String id, java.util.Map<String, Object> updates) {
        if (id == null || id.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid product ID");
        }

        Product existingProduct = productRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        if (existingProduct.isHasVariants()) {
            if (updates.containsKey("mrp") || updates.containsKey("sellingPrice") || updates.containsKey("stockQuantity")) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Cannot quick-edit stock/price for products with variants. Please edit the variants directly.");
            }
        }

        if (updates.containsKey("mrp")) {
            Object mrpValue = updates.get("mrp");
            existingProduct.setMrp((mrpValue != null && !mrpValue.toString().isBlank()) ? Double.valueOf(mrpValue.toString()) : null);
        }

        if (updates.containsKey("sellingPrice")) {
            Object spValue = updates.get("sellingPrice");
            existingProduct.setSellingPrice((spValue != null && !spValue.toString().isBlank()) ? Double.valueOf(spValue.toString()) : null);
        }

        // Recompute effective price
        Double nextMrp = existingProduct.getMrp();
        Double nextSellingPrice = existingProduct.getSellingPrice();
        validatePricePair(nextMrp, nextSellingPrice);
        existingProduct.setEffectivePrice(nextSellingPrice != null && nextSellingPrice > 0 ? nextSellingPrice : nextMrp);

        if (updates.containsKey("manageStock")) {
            existingProduct.setManageStock(Boolean.parseBoolean(updates.get("manageStock").toString()));
        }

        if (updates.containsKey("stockQuantity")) {
            Object stockValue = updates.get("stockQuantity");
            existingProduct.setStockQuantity((stockValue != null && !stockValue.toString().isBlank()) ? parseSafeInteger(stockValue.toString()) : 0);
        }
        
        if (updates.containsKey("categoryId")) {
            existingProduct.setCategoryId(updates.get("categoryId").toString());
        }
        if (updates.containsKey("subCategoryId")) {
            existingProduct.setSubCategoryId(updates.get("subCategoryId").toString());
        }
        if (updates.containsKey("expiryDate")) {
            Object expValue = updates.get("expiryDate");
            existingProduct.setExpiryDate(expValue != null ? expValue.toString() : null);
        }
        
        if (updates.containsKey("autoOfferOnExpiry")) {
            Object value = updates.get("autoOfferOnExpiry");
            if (value instanceof Boolean) {
                existingProduct.setAutoOfferOnExpiry((Boolean) value);
            } else if (value != null) {
                existingProduct.setAutoOfferOnExpiry(Boolean.parseBoolean(value.toString()));
            }
        }
        if (updates.containsKey("expiryThresholdDays")) {
            Object val = updates.get("expiryThresholdDays");
            if (val != null && !val.toString().isBlank()) {
                existingProduct.setExpiryThresholdDays(parseSafeInteger(val.toString()));
            }
        }
        if (updates.containsKey("expiryDiscountPercent")) {
            Object val = updates.get("expiryDiscountPercent");
            if (val != null && !val.toString().isBlank()) {
                existingProduct.setExpiryDiscountPercent(Double.valueOf(val.toString()));
            }
        }
        
        reevaluateExpiryOffer(existingProduct);

        return enrich(productRepository.save(existingProduct));
    }

    private boolean isValidObjectId(String id) {
        if (id == null || id.length() != 24) return false;
        return id.matches("^[0-9a-fA-F]{24}$");
    }

    public void reevaluateExpiryOffer(Product product) {
        if (!product.isAutoOfferOnExpiry()) {
            if (product.isExpiryOffer()) {
                disableExpiryOffer(product);
            }
            return;
        }

        LocalDate expiryDate = parseExpiryDate(product.getExpiryDate());
        if (expiryDate == null || product.getStockQuantity() <= 0 || LocalDate.now().isAfter(expiryDate) || LocalDate.now().isEqual(expiryDate)) {
            if (product.isExpiryOffer()) {
                disableExpiryOffer(product);
            }
            return;
        }

        int threshold = product.getExpiryThresholdDays() != null ? product.getExpiryThresholdDays() : 7;
        LocalDate thresholdDate = LocalDate.now().plusDays(threshold);

        if (!expiryDate.isAfter(thresholdDate)) {
            // Should be on offer
            if (!product.isExpiryOffer()) {
                enableExpiryOffer(product);
            } else {
                // Already on offer, but maybe settings changed (e.g. discount %)
                updateActiveOfferPrice(product);
            }
        } else {
            // Not yet in threshold
            if (product.isExpiryOffer()) {
                disableExpiryOffer(product);
            }
        }
    }

    private void enableExpiryOffer(Product product) {
        product.setExpiryOffer(true);
        product.setOriginalSellingPrice(product.getSellingPrice());
        updateActiveOfferPrice(product);
    }

    private void updateActiveOfferPrice(Product product) {
        Double basePrice = product.getOriginalSellingPrice() != null ? product.getOriginalSellingPrice() : product.getMrp();
        if (basePrice == null) return;

        double discount = product.getExpiryDiscountPercent() != null ? product.getExpiryDiscountPercent() : 10.0;
        double discountedPrice = basePrice * (1 - (discount / 100.0));
        product.setSellingPrice(discountedPrice);
        product.setEffectivePrice(discountedPrice);
    }

    private void disableExpiryOffer(Product product) {
        product.setExpiryOffer(false);
        if (product.getOriginalSellingPrice() != null) {
            product.setSellingPrice(product.getOriginalSellingPrice());
            product.setEffectivePrice(product.getOriginalSellingPrice());
        } else {
            // Fallback to MRP if original selling price was null
            product.setSellingPrice(null);
            product.setEffectivePrice(product.getMrp());
        }
        product.setOriginalSellingPrice(null);
    }

    private Integer parseSafeInteger(String value) {
        if (value == null || value.isBlank()) return 0;
        try {
            // Handle decimal strings like "7.0" by parsing as double first
            return (int) Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private LocalDate parseExpiryDate(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) return null;
        String[] parts = rawValue.trim().split("-");
        try {
            if (parts.length == 3) {
                return LocalDate.of(Integer.parseInt(parts[2]), Integer.parseInt(parts[1]), Integer.parseInt(parts[0]));
            }
            if (parts.length == 2) {
                return YearMonth.of(Integer.parseInt(parts[1]), Integer.parseInt(parts[0])).atEndOfMonth();
            }
        } catch (Exception ignored) {}
        return null;
    }
}
