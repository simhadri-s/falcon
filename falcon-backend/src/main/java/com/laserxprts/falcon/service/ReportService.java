package com.laserxprts.falcon.service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.laserxprts.falcon.dto.response.ReportDashboardResponse;
import com.laserxprts.falcon.model.Order;
import com.laserxprts.falcon.model.OrderItem;
import com.laserxprts.falcon.model.Product;
import com.laserxprts.falcon.model.ProductSnapshot;
import com.laserxprts.falcon.model.User;
import com.laserxprts.falcon.repository.OrderRepository;
import com.laserxprts.falcon.repository.ProductRepository;
import com.laserxprts.falcon.repository.RoleRepository;
import com.laserxprts.falcon.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReportService {

    private static final int LOW_STOCK_THRESHOLD = 20;
    private static final int EXPIRY_NOTICE_DAYS = 30;

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    private static final DecimalFormat MONEY_FORMAT = new DecimalFormat("#,##,##0.00");
    private static final float PAGE_MARGIN = 50f;

    public record PdfReportDownload(String fileName, byte[] content) {}

    public PdfReportDownload generatePdfReport(String periodKey) {
        ReportDashboardResponse data = getDashboardReport(periodKey);
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            
            new PdfReportWriter(document, data).write();
            
            document.save(outputStream);
            String fileName = "Management_Report_" + data.periodKey() + "_" + LocalDate.now() + ".pdf";
            return new PdfReportDownload(fileName, outputStream.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException("Error generating PDF report: " + e.getMessage(), e);
        }
    }

    public ReportDashboardResponse getDashboardReport(String periodKey) {
        PeriodSelection period = resolvePeriod(periodKey);
        List<Order> filteredOrders;
        if (period.allTime) {
            filteredOrders = orderRepository.findAllByOrderByCreatedAtDesc();
        } else {
            filteredOrders = orderRepository.findByCreatedAtGreaterThanEqualOrderByCreatedAtDesc(period.startDate);
        }

        List<Product> products = productRepository.findAll(Sort.by(Sort.Direction.ASC, "name"));
        Map<String, Product> productById = products.stream()
                .filter(product -> product.getId() != null)
                .collect(Collectors.toMap(Product::getId, product -> product, (left, right) -> left));

        List<User> roleCustomers = roleRepository.findByName("USER")
                .map(userRepository::findByRoles)
                .orElseGet(ArrayList::new);
        Map<String, User> userByEmail = roleCustomers.stream()
                .filter(user -> user.getEmail() != null)
                .collect(Collectors.toMap(User::getEmail, user -> user, (left, right) -> left));

        Set<String> missingEmails = filteredOrders.stream()
            .map(Order::getUserId)
            .filter(Objects::nonNull)
            .filter(email -> !userByEmail.containsKey(email))
            .collect(Collectors.toSet());
        if (!missingEmails.isEmpty()) {
            userRepository.findByEmailIn(missingEmails).forEach(user -> userByEmail.put(user.getEmail(), user));
        }

        Map<String, ProductSalesAggregate> productSales = new HashMap<>();
        Map<String, CategoryAggregate> categorySales = new HashMap<>();
        Map<String, CustomerAggregate> customerSales = new HashMap<>();
        Map<String, LocationAggregate> destinationSales = new HashMap<>();
        Map<String, LocationAggregate> customerCitySales = new HashMap<>();
        Map<String, StatusAggregate> statusSales = new HashMap<>();

        double grossRevenue = 0;
        double netRevenue = 0;
        double discountAmount = 0;
        double shippingRevenue = 0;
        double deliveredRevenue = 0;

        for (Order order : filteredOrders) {
            double itemsSubtotal = calculateItemsSubtotal(order);
            double orderTotal = calculateOrderTotal(order);
            long itemUnits = calculateUnits(order);
            String status = normalizeStatus(order.getStatus());
            String city = extractCity(order);

            grossRevenue += itemsSubtotal;
            netRevenue += orderTotal;
            discountAmount += defaultDouble(order.getDiscountAmount());
            shippingRevenue += defaultDouble(order.getDeliveryCharge());

            statusSales.computeIfAbsent(status, key -> new StatusAggregate()).add(orderTotal);
            destinationSales.computeIfAbsent(city, key -> new LocationAggregate()).add(orderTotal);

            if ("DELIVERED".equals(status)) {
                deliveredRevenue += orderTotal;
            }

            CustomerAggregate customerAggregate = customerSales.computeIfAbsent(defaultLabel(order.getUserId(), "Guest"), key -> new CustomerAggregate());
            customerAggregate.email = defaultLabel(order.getUserId(), "Guest");
            customerAggregate.orders++;
            customerAggregate.units += itemUnits;
            customerAggregate.spend += orderTotal;
            customerAggregate.lastOrderAt = latest(customerAggregate.lastOrderAt, order.getCreatedAt());
            customerAggregate.city = city;

            for (OrderItem item : safeItems(order)) {
                ProductSnapshot snapshot = item.getProductSnapshot();
                if (snapshot == null) {
                    continue;
                }

                String productId = defaultLabel(snapshot.getId(), snapshot.getProductCode());
                String categoryName = resolveCategory(productById.get(snapshot.getId()), snapshot);
                double lineRevenue = safePrice(snapshot) * Math.max(item.getQuantity(), 0);

                ProductSalesAggregate productAggregate = productSales.computeIfAbsent(productId, key -> new ProductSalesAggregate());
                productAggregate.productId = snapshot.getId();
                productAggregate.productCode = snapshot.getProductCode();
                productAggregate.name = defaultLabel(snapshot.getName(), "Untitled Product");
                productAggregate.category = categoryName;
                productAggregate.imageUrl = firstImage(snapshot.getImageUrls());
                productAggregate.unitsSold += Math.max(item.getQuantity(), 0);
                productAggregate.orderCount++;
                productAggregate.revenue += lineRevenue;

                CategoryAggregate categoryAggregate = categorySales.computeIfAbsent(categoryName, key -> new CategoryAggregate());
                categoryAggregate.category = categoryName;
                categoryAggregate.unitsSold += Math.max(item.getQuantity(), 0);
                categoryAggregate.revenue += lineRevenue;
            }
        }

        Map<String, Long> categoryProductCounts = products.stream()
                .collect(Collectors.groupingBy(
                        product -> defaultLabel(product.getCategory(), "Uncategorized"),
                        Collectors.counting()));
        categoryProductCounts.forEach((category, count) -> categorySales
                .computeIfAbsent(category, key -> new CategoryAggregate())
                .products = count);

        Set<String> allCustomerEmails = new HashSet<>(userByEmail.keySet());
        allCustomerEmails.addAll(orderRepository.findDistinctUserIds());

        for (CustomerAggregate aggregate : customerSales.values()) {
            User user = userByEmail.get(aggregate.email);
            if (user != null && user.getName() != null && !user.getName().isBlank()) {
                aggregate.name = user.getName();
            }
            customerCitySales.computeIfAbsent(defaultLabel(aggregate.city, "Unknown city"), key -> new LocationAggregate())
                    .add(aggregate.spend);
        }

        List<ProductSalesAggregate> bestSelling = productSales.values().stream()
                .sorted(Comparator
                        .comparingLong(ProductSalesAggregate::unitsSold).reversed()
                        .thenComparing(Comparator.comparingDouble(ProductSalesAggregate::revenue).reversed()))
                .limit(6)
                .toList();

        List<ProductSalesAggregate> leastSelling = products.stream()
                .map(product -> {
                    ProductSalesAggregate existing = productSales.getOrDefault(product.getId(), new ProductSalesAggregate());
                    if (existing.productId == null) {
                        existing.productId = product.getId();
                        existing.productCode = product.getProductCode();
                        existing.name = defaultLabel(product.getName(), "Untitled Product");
                        existing.category = defaultLabel(product.getCategory(), "Uncategorized");
                        existing.imageUrl = firstImage(product.getImageUrls());
                    }
                    return existing;
                })
                .sorted(Comparator
                        .comparingLong(ProductSalesAggregate::unitsSold)
                        .thenComparing(ProductSalesAggregate::name, String.CASE_INSENSITIVE_ORDER))
                .limit(6)
                .toList();

        long unsoldProducts = products.stream()
                .filter(product -> !productSales.containsKey(product.getId()))
                .count();

        double inventoryValue = round(products.stream()
                .filter(Product::isManageStock)
                .mapToDouble(product -> {
                    int quantity = safeInt(product.getStockQuantity());
                    return quantity * safePrice(product);
                })
                .sum());

        List<Product> trackedProducts = products.stream()
                .filter(Product::isManageStock)
                .toList();
        List<Product> lowStockProducts = trackedProducts.stream()
                .filter(product -> {
                    int quantity = safeInt(product.getStockQuantity());
                    return quantity > 0 && quantity < LOW_STOCK_THRESHOLD;
                })
                .sorted(Comparator.comparingInt(product -> safeInt(product.getStockQuantity())))
                .toList();
        List<Product> outOfStockProducts = trackedProducts.stream()
                .filter(product -> safeInt(product.getStockQuantity()) <= 0)
                .sorted(Comparator.comparing(product -> defaultLabel(product.getName(), "")))
                .toList();
        List<Product> expiringSoonProducts = products.stream()
                .filter(product -> parseExpiryDate(product.getExpiryDate()) != null)
                .filter(product -> {
                    LocalDate expiryDate = parseExpiryDate(product.getExpiryDate());
                    LocalDate threshold = LocalDate.now().plusDays(EXPIRY_NOTICE_DAYS);
                    return expiryDate != null && !expiryDate.isAfter(threshold);
                })
                .sorted(Comparator.comparing(product -> parseExpiryDate(product.getExpiryDate())))
                .toList();

        ReportDashboardResponse.Overview overview = new ReportDashboardResponse.Overview(
                filteredOrders.size(),
                allCustomerEmails.size(),
                products.size(),
                round(netRevenue),
                average(netRevenue, filteredOrders.size()),
                lowStockProducts.size());

        ReportDashboardResponse.SalesReport salesReport = new ReportDashboardResponse.SalesReport(
                round(grossRevenue),
                round(netRevenue),
                round(discountAmount),
                round(shippingRevenue),
                round(deliveredRevenue),
                average(netRevenue, filteredOrders.size()),
                buildTimeline(filteredOrders, period),
                statusSales.entrySet().stream()
                        .sorted(Map.Entry.comparingByKey())
                        .map(entry -> new ReportDashboardResponse.StatusRevenue(
                                entry.getKey(),
                                entry.getValue().count,
                                round(entry.getValue().revenue)))
                        .toList());

        long completedOrders = filteredOrders.stream()
                .filter(order -> "DELIVERED".equals(normalizeStatus(order.getStatus())))
                .count();
        long cancelledOrders = filteredOrders.stream()
                .filter(order -> "CANCELLED".equals(normalizeStatus(order.getStatus())))
                .count();
        long openOrders = Math.max(filteredOrders.size() - completedOrders - cancelledOrders, 0);

        ReportDashboardResponse.OrderReport orderReport = new ReportDashboardResponse.OrderReport(
                filteredOrders.size(),
                openOrders,
                completedOrders,
                cancelledOrders,
                filteredOrders.isEmpty() ? 0 : round((completedOrders * 100.0) / filteredOrders.size()),
                statusSales.entrySet().stream()
                        .sorted((left, right) -> Long.compare(right.getValue().count, left.getValue().count))
                        .map(entry -> new ReportDashboardResponse.StatusRevenue(
                                entry.getKey(),
                                entry.getValue().count,
                                round(entry.getValue().revenue)))
                        .toList(),
                destinationSales.entrySet().stream()
                        .sorted((left, right) -> Long.compare(right.getValue().count, left.getValue().count))
                        .limit(5)
                        .map(entry -> new ReportDashboardResponse.LocationBreakdown(
                                entry.getKey(),
                                entry.getValue().count,
                                round(entry.getValue().revenue)))
                        .toList(),
                filteredOrders.stream()
                        .sorted(Comparator.comparing(Order::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                        .limit(8)
                        .map(order -> new ReportDashboardResponse.RecentOrder(
                                order.getId(),
                                order.getUserId(),
                                normalizeStatus(order.getStatus()),
                                round(calculateOrderTotal(order)),
                                calculateUnits(order),
                                order.getCreatedAt(),
                                extractCity(order)))
                        .toList());

        ReportDashboardResponse.ProductReport productReport = new ReportDashboardResponse.ProductReport(
                products.size(),
                products.stream().filter(Product::isPublished).count(),
                products.stream().filter(Product::isFeatured).count(),
                unsoldProducts,
                inventoryValue,
                bestSelling.stream()
                        .map(this::toProductPerformance)
                        .toList(),
                leastSelling.stream()
                        .map(this::toProductPerformance)
                        .toList(),
                categorySales.values().stream()
                        .sorted(Comparator.comparingDouble(CategoryAggregate::revenue).reversed())
                        .limit(6)
                        .map(category -> new ReportDashboardResponse.CategoryPerformance(
                                category.category,
                                category.unitsSold,
                                category.products,
                                round(category.revenue)))
                        .toList());

        long activeCustomers = customerSales.size();
        long repeatCustomers = customerSales.values().stream()
                .filter(customer -> customer.orders > 1)
                .count();

        ReportDashboardResponse.CustomerReport customerReport = new ReportDashboardResponse.CustomerReport(
                allCustomerEmails.size(),
                activeCustomers,
                repeatCustomers,
                activeCustomers == 0 ? 0 : round((repeatCustomers * 100.0) / activeCustomers),
                average(netRevenue, activeCustomers),
                customerSales.values().stream()
                        .sorted(Comparator.comparingDouble(CustomerAggregate::spend).reversed())
                        .limit(6)
                        .map(customer -> new ReportDashboardResponse.CustomerValue(
                                customer.email,
                                defaultLabel(customer.name, customer.email),
                                customer.orders,
                                customer.units,
                                round(customer.spend),
                                customer.lastOrderAt,
                                defaultLabel(customer.city, "Unknown city")))
                        .toList(),
                customerCitySales.entrySet().stream()
                        .sorted((left, right) -> Long.compare(right.getValue().count, left.getValue().count))
                        .limit(5)
                        .map(entry -> new ReportDashboardResponse.LocationBreakdown(
                                entry.getKey(),
                                entry.getValue().count,
                                round(entry.getValue().revenue)))
                        .toList());

        ReportDashboardResponse.StockReport stockReport = new ReportDashboardResponse.StockReport(
                trackedProducts.size(),
                trackedProducts.stream().filter(product -> safeInt(product.getStockQuantity()) > 0).count(),
                lowStockProducts.size(),
                outOfStockProducts.size(),
                expiringSoonProducts.size(),
                inventoryValue,
                lowStockProducts.stream().limit(8).map(this::toStockItem).toList(),
                outOfStockProducts.stream().limit(8).map(this::toStockItem).toList(),
                expiringSoonProducts.stream().limit(8).map(this::toStockItem).toList());

        return new ReportDashboardResponse(
                period.key,
                period.label,
                LocalDateTime.now(),
                overview,
                salesReport,
                orderReport,
                productReport,
                customerReport,
                stockReport);
    }

    private ReportDashboardResponse.ProductPerformance toProductPerformance(ProductSalesAggregate aggregate) {
        return new ReportDashboardResponse.ProductPerformance(
                aggregate.productId,
                aggregate.productCode,
                aggregate.name,
                aggregate.unitsSold,
                aggregate.orderCount,
                round(aggregate.revenue),
                aggregate.category,
                aggregate.imageUrl);
    }

    private ReportDashboardResponse.StockItem toStockItem(Product product) {
        return new ReportDashboardResponse.StockItem(
                product.getId(),
                product.getProductCode(),
                product.getName(),
                product.getStockQuantity(),
                product.isManageStock(),
                product.getExpiryDate(),
                round(safePrice(product)),
                defaultLabel(product.getCategory(), "Uncategorized"),
                firstImage(product.getImageUrls()));
    }

    private List<ReportDashboardResponse.TimelinePoint> buildTimeline(List<Order> orders, PeriodSelection period) {
        if (period.allTime) {
            Map<YearMonth, StatusAggregate> monthlyRevenue = new LinkedHashMap<>();
            LocalDate currentMonth = LocalDate.now().withDayOfMonth(1);
            for (int index = 11; index >= 0; index--) {
                YearMonth yearMonth = YearMonth.from(currentMonth.minusMonths(index));
                monthlyRevenue.put(yearMonth, new StatusAggregate());
            }

            for (Order order : orders) {
                if (order.getCreatedAt() == null) {
                    continue;
                }
                YearMonth yearMonth = YearMonth.from(order.getCreatedAt());
                if (monthlyRevenue.containsKey(yearMonth)) {
                    monthlyRevenue.get(yearMonth).add(calculateOrderTotal(order));
                }
            }

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH);
            return monthlyRevenue.entrySet().stream()
                    .map(entry -> new ReportDashboardResponse.TimelinePoint(
                            entry.getKey().atDay(1).format(formatter),
                            round(entry.getValue().revenue),
                            entry.getValue().count))
                    .toList();
        }

        Map<LocalDate, StatusAggregate> dailyRevenue = new LinkedHashMap<>();
        LocalDate startDate = period.startDate.toLocalDate();
        LocalDate endDate = LocalDate.now();
        LocalDate pointer = startDate;
        while (!pointer.isAfter(endDate)) {
            dailyRevenue.put(pointer, new StatusAggregate());
            pointer = pointer.plusDays(1);
        }

        for (Order order : orders) {
            if (order.getCreatedAt() == null) {
                continue;
            }
            LocalDate orderDate = order.getCreatedAt().toLocalDate();
            if (dailyRevenue.containsKey(orderDate)) {
                dailyRevenue.get(orderDate).add(calculateOrderTotal(order));
            }
        }

        return dailyRevenue.entrySet().stream()
                .map(entry -> new ReportDashboardResponse.TimelinePoint(
                        entry.getKey().getDayOfMonth() + " " + entry.getKey().getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH),
                        round(entry.getValue().revenue),
                        entry.getValue().count))
                .toList();
    }

    private PeriodSelection resolvePeriod(String requestedPeriod) {
        String key = requestedPeriod == null ? "30d" : requestedPeriod.trim().toLowerCase(Locale.ENGLISH);
        LocalDateTime now = LocalDateTime.now();
        return switch (key) {
            case "7d" -> new PeriodSelection("7d", "Last 7 Days", now.minusDays(6), false);
            case "90d" -> new PeriodSelection("90d", "Last 90 Days", now.minusDays(89), false);
            case "all" -> new PeriodSelection("all", "All Time", null, true);
            default -> new PeriodSelection("30d", "Last 30 Days", now.minusDays(29), false);
        };
    }

    private boolean isWithinPeriod(Order order, PeriodSelection period) {
        if (period.allTime) {
            return true;
        }
        return order.getCreatedAt() != null && !order.getCreatedAt().isBefore(period.startDate);
    }

    private double calculateItemsSubtotal(Order order) {
        return round(safeItems(order).stream()
                .mapToDouble(item -> safePrice(item.getProductSnapshot()) * Math.max(item.getQuantity(), 0))
                .sum());
    }

    private double calculateOrderTotal(Order order) {
        return round(calculateItemsSubtotal(order) + defaultDouble(order.getDeliveryCharge()) - defaultDouble(order.getDiscountAmount()));
    }

    private long calculateUnits(Order order) {
        return safeItems(order).stream()
                .mapToLong(item -> Math.max(item.getQuantity(), 0))
                .sum();
    }

    private List<OrderItem> safeItems(Order order) {
        return order.getItems() == null ? List.of() : order.getItems();
    }

    private String normalizeStatus(String status) {
        return defaultLabel(status, "UNKNOWN").trim().toUpperCase(Locale.ENGLISH);
    }

    private String defaultLabel(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private double defaultDouble(Double value) {
        return value == null ? 0 : value;
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private double safePrice(ProductSnapshot snapshot) {
        if (snapshot == null) {
            return 0;
        }
        return snapshot.getSellingPrice() != null && snapshot.getSellingPrice() > 0
                ? snapshot.getSellingPrice()
                : defaultDouble(snapshot.getMrp());
    }

    private double safePrice(Product product) {
        if (product == null) {
            return 0;
        }
        return product.getSellingPrice() != null && product.getSellingPrice() > 0
                ? product.getSellingPrice()
                : defaultDouble(product.getMrp());
    }

    private String resolveCategory(Product product, ProductSnapshot snapshot) {
        if (product != null && product.getCategory() != null && !product.getCategory().isBlank()) {
            return product.getCategory();
        }
        return defaultLabel(snapshot != null ? snapshot.getCategoryId() : null, "Uncategorized");
    }

    private String extractCity(Order order) {
        if (order.getAddressSnapshot() == null) {
            return "Unknown city";
        }
        return defaultLabel(order.getAddressSnapshot().getCity(), "Unknown city");
    }

    private String firstImage(List<String> imageUrls) {
        return imageUrls == null || imageUrls.isEmpty() ? null : imageUrls.getFirst();
    }

    private LocalDate parseExpiryDate(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }

        String[] parts = rawValue.trim().split("-");
        try {
            if (parts.length == 3) {
                int day = Integer.parseInt(parts[0]);
                int month = Integer.parseInt(parts[1]);
                int year = Integer.parseInt(parts[2]);
                return LocalDate.of(year, month, day);
            }

            if (parts.length == 2) {
                int month = Integer.parseInt(parts[0]);
                int year = Integer.parseInt(parts[1]);
                return YearMonth.of(year, month).atEndOfMonth();
            }
        } catch (RuntimeException ignored) {
            return null;
        }

        return null;
    }

    private LocalDateTime latest(LocalDateTime current, LocalDateTime incoming) {
        if (current == null) {
            return incoming;
        }
        if (incoming == null) {
            return current;
        }
        return incoming.isAfter(current) ? incoming : current;
    }

    private double average(double total, long divisor) {
        if (divisor <= 0) {
            return 0;
        }
        return round(total / divisor);
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private record PeriodSelection(String key, String label, LocalDateTime startDate, boolean allTime) {
    }

    private static final class StatusAggregate {
        private long count;
        private double revenue;

        private void add(double amount) {
            this.count++;
            this.revenue += amount;
        }
    }

    private static final class LocationAggregate {
        private long count;
        private double revenue;

        private void add(double amount) {
            this.count++;
            this.revenue += amount;
        }
    }

    private static final class ProductSalesAggregate {
        private String productId;
        private String productCode;
        private String name;
        private long unitsSold;
        private long orderCount;
        private double revenue;
        private String category;
        private String imageUrl;

        private long unitsSold() {
            return unitsSold;
        }

        private double revenue() {
            return revenue;
        }

        private String name() {
            return defaultText(name);
        }
    }

    private static final class CategoryAggregate {
        private String category;
        private long unitsSold;
        private long products;
        private double revenue;

        private double revenue() {
            return revenue;
        }
    }

    private static final class CustomerAggregate {
        private String email;
        private String name;
        private long orders;
        private long units;
        private double spend;
        private LocalDateTime lastOrderAt;
        private String city;

        private double spend() {
            return spend;
        }
    }

    private static String defaultText(String value) {
        return value == null ? "" : value;
    }

    private final class PdfReportWriter {
        private final PDDocument document;
        private final ReportDashboardResponse data;
        private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a", Locale.ENGLISH);
        private final Color FALCON_NAVY = new Color(30, 58, 95);
        private final Color LIGHT_BLUE = new Color(235, 245, 255);

        private PDPage page;
        private PDPageContentStream contentStream;
        private float cursorY;

        private PdfReportWriter(PDDocument document, ReportDashboardResponse data) {
            this.document = document;
            this.data = data;
        }

        public void write() throws IOException {
            startNewPage();
            drawHeader();
            
            drawSectionHeader("Executive Summary");
            drawMetricGrid();
            
            drawSectionHeader("Sales Performance");
            drawSalesTableWithChart();
            
            if (cursorY < 250) startNewPage();
            drawSectionHeader("Order Analytics");
            drawOrderMetrics();
            
            startNewPage();
            drawSectionHeader("Inventory & Stock Status");
            drawStockTable();
            
            drawSectionHeader("Customer Insights");
            drawCustomerSummary();
            
            drawFooter();
            closeCurrentPage();
        }

        private void startNewPage() throws IOException {
            closeCurrentPage();
            page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            contentStream = new PDPageContentStream(document, page);
            cursorY = page.getMediaBox().getHeight() - PAGE_MARGIN;
        }

        private void closeCurrentPage() throws IOException {
            if (contentStream != null) {
                contentStream.close();
                contentStream = null;
            }
        }

        private void drawHeader() throws IOException {
            // Draw a navy header bar
            contentStream.setNonStrokingColor(FALCON_NAVY);
            contentStream.addRect(0, page.getMediaBox().getHeight() - 80, page.getMediaBox().getWidth(), 80);
            contentStream.fill();

            contentStream.setNonStrokingColor(Color.WHITE);
            drawText("FALCON BUSINESS INTELLIGENCE", PAGE_MARGIN, page.getMediaBox().getHeight() - 40, PDType1Font.HELVETICA_BOLD, 20);
            drawText("Management Performance Report", PAGE_MARGIN, page.getMediaBox().getHeight() - 60, PDType1Font.HELVETICA, 10);
            
            contentStream.setNonStrokingColor(Color.BLACK);
            cursorY = page.getMediaBox().getHeight() - 110;
            drawText("Reporting Period: " + data.periodLabel(), PAGE_MARGIN, cursorY, PDType1Font.HELVETICA_BOLD, 12);
            cursorY -= 15;
            drawText("Generated On: " + data.generatedAt().format(dateFormatter), PAGE_MARGIN, cursorY, PDType1Font.HELVETICA, 10);
            cursorY -= 25;
            drawDivider();
            cursorY -= 20;
        }

        private void drawSectionHeader(String title) throws IOException {
            if (cursorY < 120) startNewPage();
            cursorY -= 15;
            contentStream.setNonStrokingColor(FALCON_NAVY);
            contentStream.addRect(PAGE_MARGIN, cursorY - 2, 3, 16);
            contentStream.fill();
            
            contentStream.setNonStrokingColor(FALCON_NAVY);
            drawText(title.toUpperCase(), PAGE_MARGIN + 10, cursorY, PDType1Font.HELVETICA_BOLD, 13);
            contentStream.setNonStrokingColor(Color.BLACK);
            cursorY -= 25;
        }

        private void drawMetricGrid() throws IOException {
            float boxWidth = (page.getMediaBox().getWidth() - (PAGE_MARGIN * 2) - 20) / 3;
            float boxHeight = 50;
            float startX = PAGE_MARGIN;
            
            drawMetricBox(startX, cursorY, boxWidth, boxHeight, "Total Revenue", formatMoney(data.overview().totalRevenue()));
            drawMetricBox(startX + boxWidth + 10, cursorY, boxWidth, boxHeight, "Total Orders", String.valueOf(data.overview().totalOrders()));
            drawMetricBox(startX + (boxWidth + 10) * 2, cursorY, boxWidth, boxHeight, "Avg Order Value", formatMoney(data.overview().averageOrderValue()));
            
            cursorY -= (boxHeight + 15);
            
            drawMetricBox(startX, cursorY, boxWidth, boxHeight, "Unique Customers", String.valueOf(data.overview().totalCustomers()));
            drawMetricBox(startX + boxWidth + 10, cursorY, boxWidth, boxHeight, "Total Products", String.valueOf(data.overview().totalProducts()));
            drawMetricBox(startX + (boxWidth + 10) * 2, cursorY, boxWidth, boxHeight, "Low Stock SKUs", String.valueOf(data.overview().lowStockProducts()));
            
            cursorY -= (boxHeight + 20);
        }

        private void drawMetricBox(float x, float y, float w, float h, String label, String value) throws IOException {
            contentStream.setNonStrokingColor(LIGHT_BLUE);
            contentStream.addRect(x, y - h, w, h);
            contentStream.fill();
            
            contentStream.setNonStrokingColor(FALCON_NAVY);
            drawText(label.toUpperCase(), x + 8, y - 18, PDType1Font.HELVETICA_BOLD, 8);
            contentStream.setNonStrokingColor(Color.BLACK);
            drawText(value, x + 8, y - 40, PDType1Font.HELVETICA_BOLD, 14);
        }

        private void drawSalesTableWithChart() throws IOException {
            float tableWidth = 300;
            float chartWidth = 180;
            float startX = PAGE_MARGIN;
            
            // Draw Table
            drawRow(startX, cursorY, "Status", "Orders", "Revenue", true);
            for (ReportDashboardResponse.StatusRevenue sr : data.salesReport().statusRevenue()) {
                drawRow(startX, cursorY, sr.status(), String.valueOf(sr.count()), formatMoney(sr.revenue()), false);
            }
            
            // Simple Bar Chart on the right
            float chartX = page.getMediaBox().getWidth() - PAGE_MARGIN - chartWidth;
            float chartY = cursorY + (data.salesReport().statusRevenue().size() * 15) + 15;
            drawStatusBarChart(chartX, chartY, chartWidth, 80);
            
            cursorY -= 20;
        }

        private void drawStatusBarChart(float x, float y, float w, float h) throws IOException {
            List<ReportDashboardResponse.StatusRevenue> items = data.salesReport().statusRevenue();
            if (items.isEmpty()) return;
            
            double maxRevenue = items.stream().mapToDouble(ReportDashboardResponse.StatusRevenue::revenue).max().orElse(1.0);
            float barHeight = 10;
            float gap = 8;
            float currentY = y - 20;
            
            drawText("REVENUE BY STATUS", x, y, PDType1Font.HELVETICA_BOLD, 9);
            
            for (ReportDashboardResponse.StatusRevenue sr : items) {
                float normalizedWidth = (float) (sr.revenue() / maxRevenue) * (w - 40);
                
                contentStream.setNonStrokingColor(new Color(200, 200, 200));
                contentStream.addRect(x, currentY, w - 40, barHeight);
                contentStream.fill();
                
                contentStream.setNonStrokingColor(FALCON_NAVY);
                contentStream.addRect(x, currentY, normalizedWidth, barHeight);
                contentStream.fill();
                
                contentStream.setNonStrokingColor(Color.BLACK);
                drawText(sr.status(), x, currentY + barHeight + 2, PDType1Font.HELVETICA, 7);
                currentY -= (barHeight + gap);
            }
        }

        private void drawOrderMetrics() throws IOException {
            drawSummaryRow("Open Pipeline", String.valueOf(data.orderReport().openOrders()));
            drawSummaryRow("Completed Orders", String.valueOf(data.orderReport().completedOrders()));
            drawSummaryRow("Cancelled Orders", String.valueOf(data.orderReport().cancelledOrders()));
            drawSummaryRow("Fulfillment Rate", data.orderReport().fulfillmentRate() + "%");
            cursorY -= 10;
        }

        private void drawStockTable() throws IOException {
            drawSummaryRow("Tracked SKUs", String.valueOf(data.stockReport().totalTrackedProducts()));
            drawSummaryRow("In Stock", String.valueOf(data.stockReport().inStockProducts()));
            drawSummaryRow("Out of Stock", String.valueOf(data.stockReport().outOfStockProducts()));
            drawSummaryRow("Inventory Value", formatMoney(data.stockReport().inventoryValue()));
            cursorY -= 20;

            if (!data.stockReport().lowStockItems().isEmpty()) {
                contentStream.setNonStrokingColor(new Color(255, 245, 245));
                contentStream.addRect(PAGE_MARGIN, cursorY - 140, page.getMediaBox().getWidth() - (PAGE_MARGIN * 2), 150);
                contentStream.fill();
                contentStream.setNonStrokingColor(Color.BLACK);

                drawText("CRITICAL LOW STOCK ITEMS", PAGE_MARGIN + 10, cursorY, PDType1Font.HELVETICA_BOLD, 10);
                cursorY -= 20;
                drawRow(PAGE_MARGIN + 10, cursorY, "Product", "SKU", "Stock", true);
                for (ReportDashboardResponse.StockItem item : data.stockReport().lowStockItems()) {
                    drawRow(PAGE_MARGIN + 10, cursorY, truncate(item.name(), 35), item.productCode(), String.valueOf(item.stockQuantity()), false);
                }
            }
            cursorY -= 10;
        }

        private void drawCustomerSummary() throws IOException {
            drawSummaryRow("Total Registered Users", String.valueOf(data.customerReport().totalCustomers()));
            drawSummaryRow("Active (This Period)", String.valueOf(data.customerReport().activeCustomers()));
            drawSummaryRow("Repeat Customer Rate", data.customerReport().repeatCustomerRate() + "%");
            drawSummaryRow("Average Customer LTV", formatMoney(data.customerReport().averageCustomerValue()));
            cursorY -= 10;
        }

        private void drawFooter() throws IOException {
            cursorY = 50;
            drawDivider();
            contentStream.setNonStrokingColor(new Color(120, 120, 120));
            drawText("Falcon Business Intelligence - Internal Management Document", PAGE_MARGIN, 35, PDType1Font.HELVETICA_OBLIQUE, 8);
            drawText("Confidential", page.getMediaBox().getWidth() - PAGE_MARGIN - 50, 35, PDType1Font.HELVETICA, 8);
            contentStream.setNonStrokingColor(Color.BLACK);
        }

        private void drawRow(float x, float y, String c1, String c2, String c3, boolean bold) throws IOException {
            if (cursorY < 50) startNewPage();
            PDType1Font font = bold ? PDType1Font.HELVETICA_BOLD : PDType1Font.HELVETICA;
            drawText(c1, x + 10, cursorY, font, 9);
            drawText(c2, x + 180, cursorY, font, 9);
            drawText(c3, x + 250, cursorY, font, 9);
            cursorY -= 15;
        }

        private void drawSummaryRow(String label, String value) throws IOException {
            if (cursorY < 50) startNewPage();
            drawText(label + ":", PAGE_MARGIN + 20, cursorY, PDType1Font.HELVETICA, 10);
            drawText(value, PAGE_MARGIN + 180, cursorY, PDType1Font.HELVETICA_BOLD, 10);
            cursorY -= 18;
        }

        private void drawText(String text, float x, float y, PDType1Font font, float fontSize) throws IOException {
            if (text == null) text = "";
            contentStream.beginText();
            contentStream.setFont(font, fontSize);
            contentStream.newLineAtOffset(x, y);
            contentStream.showText(text);
            contentStream.endText();
        }

        private void drawDivider() throws IOException {
            contentStream.setStrokingColor(new Color(220, 220, 220));
            contentStream.moveTo(PAGE_MARGIN, cursorY);
            contentStream.lineTo(page.getMediaBox().getWidth() - PAGE_MARGIN, cursorY);
            contentStream.stroke();
            contentStream.setStrokingColor(Color.BLACK);
        }

        private String formatMoney(double amount) {
            return "INR " + MONEY_FORMAT.format(amount);
        }

        private String truncate(String text, int max) {
            if (text == null) return "";
            return text.length() <= max ? text : text.substring(0, max - 3) + "...";
        }
    }
}
