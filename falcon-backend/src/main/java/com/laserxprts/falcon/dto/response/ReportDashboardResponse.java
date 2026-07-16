package com.laserxprts.falcon.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record ReportDashboardResponse(
        String periodKey,
        String periodLabel,
        LocalDateTime generatedAt,
        Overview overview,
        SalesReport salesReport,
        OrderReport orderReport,
        ProductReport productReport,
        CustomerReport customerReport,
        StockReport stockReport) {

    public record Overview(
            long totalOrders,
            long totalCustomers,
            long totalProducts,
            double totalRevenue,
            double averageOrderValue,
            long lowStockProducts) {
    }

    public record SalesReport(
            double grossRevenue,
            double netRevenue,
            double discounts,
            double shippingRevenue,
            double deliveredRevenue,
            double averageOrderValue,
            List<TimelinePoint> timeline,
            List<StatusRevenue> statusRevenue) {
    }

    public record OrderReport(
            long totalOrders,
            long openOrders,
            long completedOrders,
            long cancelledOrders,
            double fulfillmentRate,
            List<StatusRevenue> statusBreakdown,
            List<LocationBreakdown> topDestinations,
            List<RecentOrder> recentOrders) {
    }

    public record ProductReport(
            long totalProducts,
            long publishedProducts,
            long featuredProducts,
            long unsoldProducts,
            double inventoryValue,
            List<ProductPerformance> bestSellingProducts,
            List<ProductPerformance> leastSellingProducts,
            List<CategoryPerformance> categoryPerformance) {
    }

    public record CustomerReport(
            long totalCustomers,
            long activeCustomers,
            long repeatCustomers,
            double repeatCustomerRate,
            double averageCustomerValue,
            List<CustomerValue> topCustomers,
            List<LocationBreakdown> topCustomerCities) {
    }

    public record StockReport(
            long totalTrackedProducts,
            long inStockProducts,
            long lowStockProducts,
            long outOfStockProducts,
            long expiringSoonProducts,
            double inventoryValue,
            List<StockItem> lowStockItems,
            List<StockItem> outOfStockItems,
            List<StockItem> expiringSoonItems) {
    }

    public record TimelinePoint(String label, double value, long orders) {
    }

    public record StatusRevenue(String status, long count, double revenue) {
    }

    public record LocationBreakdown(String label, long count, double revenue) {
    }

    public record RecentOrder(
            String orderId,
            String customerEmail,
            String status,
            double total,
            long items,
            LocalDateTime createdAt,
            String city) {
    }

    public record ProductPerformance(
            String productId,
            String productCode,
            String name,
            long unitsSold,
            long orderCount,
            double revenue,
            String category,
            String imageUrl) {
    }

    public record CategoryPerformance(
            String category,
            long unitsSold,
            long products,
            double revenue) {
    }

    public record CustomerValue(
            String customerEmail,
            String customerName,
            long orders,
            long units,
            double spend,
            LocalDateTime lastOrderAt,
            String city) {
    }

    public record StockItem(
            String productId,
            String productCode,
            String name,
            Integer stockQuantity,
            boolean manageStock,
            String expiryDate,
            double unitPrice,
            String category,
            String imageUrl) {
    }
}
