package com.laserxprts.falcon.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.stereotype.Service;

import com.laserxprts.falcon.model.AddressSnapshot;
import com.laserxprts.falcon.model.CompanyImage;
import com.laserxprts.falcon.model.CompanySettings;
import com.laserxprts.falcon.model.Order;
import com.laserxprts.falcon.model.OrderItem;
import com.laserxprts.falcon.model.ReceiptDocument;
import com.laserxprts.falcon.repository.CompanyImageRepository;
import com.laserxprts.falcon.repository.CompanySettingsRepository;
import com.laserxprts.falcon.repository.OrderRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrderReceiptService {

    private static final String COMPANY_SETTINGS_ID = "COMPANY_SETTINGS";
    private static final float PAGE_MARGIN = 40f;
    private static final float PAGE_BOTTOM_MARGIN = 60f;
    private static final DecimalFormat MONEY_FORMAT = new DecimalFormat("0.00");

    private final OrderRepository orderRepository;
    private final CompanySettingsRepository companySettingsRepository;
    private final CompanyImageRepository companyImageRepository;
    private final FileUploadService fileUploadService;

    public record ReceiptDownload(String fileName, byte[] content) {}

    public Order ensureReceiptAvailable(Order order) {
        if (order == null || order.getId() == null || order.getId().isBlank()) {
            throw new RuntimeException("Order details are not available for receipt generation.");
        }

        if (hasStoredReceipt(order)) {
            return order;
        }

        byte[] receiptPdf = buildReceiptPdf(order);
        String fileName = buildFileName(order);
        FileUploadService.UploadedAsset uploadedAsset = fileUploadService.uploadReceiptPdf(
            receiptPdf,
            fileName,
            buildPublicId(order)
        );

        order.setReceipt(ReceiptDocument.builder()
            .fileName(fileName)
            .generatedAt(LocalDateTime.now())
            .url(uploadedAsset.secureUrl())
            .publicId(uploadedAsset.publicId())
            .build());

        return orderRepository.save(order);
    }

    public ReceiptDownload downloadReceipt(Order order) {
        Order receiptReadyOrder = ensureReceiptAvailable(order);
        ReceiptDocument receipt = receiptReadyOrder.getReceipt();
        if (receipt == null || receipt.getUrl() == null || receipt.getUrl().isBlank()) {
            throw new RuntimeException("Receipt is not available for this order.");
        }

        try {
            return new ReceiptDownload(resolveFileName(receiptReadyOrder), fileUploadService.downloadFile(receipt.getUrl()));
        } catch (RuntimeException firstDownloadFailure) {
            receiptReadyOrder.setReceipt(null);
            Order regeneratedOrder = ensureReceiptAvailable(receiptReadyOrder);
            return new ReceiptDownload(
                resolveFileName(regeneratedOrder),
                fileUploadService.downloadFile(regeneratedOrder.getReceipt().getUrl())
            );
        }
    }

    private boolean hasStoredReceipt(Order order) {
        return order.getReceipt() != null
            && order.getReceipt().getUrl() != null
            && !order.getReceipt().getUrl().isBlank();
    }

    private byte[] buildReceiptPdf(Order order) {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            ReceiptCompanyDetails company = loadCompanyDetails();
            ReceiptTotals totals = calculateTotals(order);
            new ReceiptPageWriter(document, company, order, totals).write();
            document.save(outputStream);
            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Unable to generate receipt PDF: " + e.getMessage(), e);
        }
    }

    private ReceiptCompanyDetails loadCompanyDetails() {
        Optional<CompanySettings> settingsOptional = companySettingsRepository.findById(COMPANY_SETTINGS_ID);
        CompanySettings settings = settingsOptional.orElseGet(CompanySettings::new);

        String logoUrl = clean(settings.getLogoUrl());
        if (logoUrl == null) {
            logoUrl = companyImageRepository.findAll().stream()
                .map(CompanyImage::getLogoUrl)
                .map(this::clean)
                .filter(value -> value != null && !value.isBlank())
                .findFirst()
                .orElse(null);
        }

        return new ReceiptCompanyDetails(
            defaultIfBlank(settings.getCompanyName(), "Falcon Store"),
            defaultIfBlank(settings.getEmail(), ""),
            defaultIfBlank(settings.getPhone(), ""),
            defaultIfBlank(settings.getAddress(), ""),
            defaultIfBlank(settings.getWorkingHours(), ""),
            defaultIfBlank(settings.getTermsAndConditions(), ""),
            logoUrl
        );
    }

    private ReceiptTotals calculateTotals(Order order) {
        double subtotal = order.getItems().stream()
            .mapToDouble(item -> resolveItemUnitPrice(item) * item.getQuantity())
            .sum();
        double deliveryCharge = order.getDeliveryCharge();
        double discount = order.getDiscountAmount();
        double grandTotal = subtotal + deliveryCharge - discount;
        return new ReceiptTotals(subtotal, deliveryCharge, discount, grandTotal);
    }

    private double resolveItemUnitPrice(OrderItem item) {
        if (item == null || item.getProductSnapshot() == null) {
            return 0.0;
        }

        Double sellingPrice = item.getProductSnapshot().getSellingPrice();
        if (sellingPrice != null && sellingPrice > 0) {
            return sellingPrice;
        }

        Double mrp = item.getProductSnapshot().getMrp();
        return mrp != null ? mrp : 0.0;
    }

    private String buildFileName(Order order) {
        return "receipt-" + order.getId() + ".pdf";
    }

    private String buildPublicId(Order order) {
        return "receipt_" + order.getId() + ".pdf";
    }

    private String resolveFileName(Order order) {
        if (order.getReceipt() != null && order.getReceipt().getFileName() != null && !order.getReceipt().getFileName().isBlank()) {
            return order.getReceipt().getFileName();
        }
        return buildFileName(order);
    }

    private String clean(String value) {
        return value == null ? null : value.trim();
    }

    private String defaultIfBlank(String value, String fallback) {
        String cleaned = clean(value);
        return cleaned == null || cleaned.isBlank() ? fallback : cleaned;
    }

    private String formatMoney(double amount) {
        return "INR " + MONEY_FORMAT.format(amount);
    }

    private final class ReceiptPageWriter {
        private final PDDocument document;
        private final ReceiptCompanyDetails company;
        private final Order order;
        private final ReceiptTotals totals;
        private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a", Locale.ENGLISH);

        private PDPage page;
        private PDPageContentStream contentStream;
        private float cursorY;

        private ReceiptPageWriter(PDDocument document, ReceiptCompanyDetails company, Order order, ReceiptTotals totals) {
            this.document = document;
            this.company = company;
            this.order = order;
            this.totals = totals;
        }

        private void write() throws IOException {
            startNewPage(false);
            drawOrderMeta();
            drawAddresses();
            drawItemsTable();
            drawTotalsBlock();
            drawFooter();
            closeCurrentPage();
        }

        private void startNewPage(boolean showContinuationHeader) throws IOException {
            closeCurrentPage();
            page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            contentStream = new PDPageContentStream(document, page);
            cursorY = page.getMediaBox().getHeight() - PAGE_MARGIN;

            drawBrandHeader(showContinuationHeader);
            drawDivider(cursorY - 8f);
            cursorY -= 22f;
        }

        private void closeCurrentPage() throws IOException {
            if (contentStream != null) {
                contentStream.close();
                contentStream = null;
            }
        }

        private void drawBrandHeader(boolean showContinuationHeader) throws IOException {
            float leftX = PAGE_MARGIN;
            float rightX = page.getMediaBox().getWidth() - PAGE_MARGIN;
            float logoBottomY = cursorY;

            byte[] logoBytes = loadLogoBytes();
            if (logoBytes != null) {
                try {
                    PDImageXObject logo = PDImageXObject.createFromByteArray(document, logoBytes, "company-logo");
                    float maxWidth = 90f;
                    float maxHeight = 55f;
                    float scale = Math.min(maxWidth / logo.getWidth(), maxHeight / logo.getHeight());
                    float drawWidth = logo.getWidth() * scale;
                    float drawHeight = logo.getHeight() * scale;
                    contentStream.drawImage(logo, leftX, cursorY - drawHeight, drawWidth, drawHeight);
                    logoBottomY = cursorY - drawHeight;
                } catch (Exception ignored) {
                    logoBottomY = cursorY;
                }
            }

            float textTopY = cursorY;
            drawText(company.companyName(), rightX - 220f, textTopY - 4f, PDType1Font.HELVETICA_BOLD, 18f);
            drawWrappedText(company.address(), rightX - 220f, textTopY - 22f, 220f, 10.5f, PDType1Font.HELVETICA);
            drawWrappedText(joinNonBlank(company.email(), company.phone()), rightX - 220f, textTopY - 48f, 220f, 10.5f, PDType1Font.HELVETICA);

            float titleY = Math.min(logoBottomY, textTopY - 58f) - 18f;
            drawText(showContinuationHeader ? "Receipt (continued)" : "Receipt", PAGE_MARGIN, titleY, PDType1Font.HELVETICA_BOLD, 24f);
            cursorY = titleY - 8f;
        }

        private void drawOrderMeta() throws IOException {
            float boxWidth = 240f;
            float boxHeight = 74f;
            float boxX = page.getMediaBox().getWidth() - PAGE_MARGIN - boxWidth;
            float boxY = cursorY - boxHeight;

            drawBox(boxX, boxY, boxWidth, boxHeight);
            drawText("Receipt No.", boxX + 12f, boxY + 54f, PDType1Font.HELVETICA_BOLD, 10f);
            drawText(order.getId(), boxX + 12f, boxY + 38f, PDType1Font.HELVETICA, 11f);
            drawText("Order Date", boxX + 12f, boxY + 22f, PDType1Font.HELVETICA_BOLD, 10f);
            drawText(formatOrderDate(), boxX + 12f, boxY + 8f, PDType1Font.HELVETICA, 11f);

            drawText("Status", PAGE_MARGIN, cursorY - 20f, PDType1Font.HELVETICA_BOLD, 11f);
            drawText(order.getStatus(), PAGE_MARGIN, cursorY - 38f, PDType1Font.HELVETICA, 11f);
            if (order.getCouponCode() != null && !order.getCouponCode().isBlank()) {
                drawText("Coupon", PAGE_MARGIN, cursorY - 56f, PDType1Font.HELVETICA_BOLD, 11f);
                drawText(order.getCouponCode(), PAGE_MARGIN + 52f, cursorY - 56f, PDType1Font.HELVETICA, 11f);
            }

            cursorY = boxY - 24f;
        }

        private void drawAddresses() throws IOException {
            AddressSnapshot address = order.getAddressSnapshot();
            float blockTopY = cursorY;

            drawText("Billed To", PAGE_MARGIN, blockTopY, PDType1Font.HELVETICA_BOLD, 12f);
            float customerY = blockTopY - 18f;
            for (String line : buildCustomerAddressLines(address)) {
                customerY = drawWrappedText(line, PAGE_MARGIN, customerY, 250f, 10.5f, PDType1Font.HELVETICA);
            }

            drawText("Company Details", PAGE_MARGIN + 290f, blockTopY, PDType1Font.HELVETICA_BOLD, 12f);
            float companyY = blockTopY - 18f;
            for (String line : buildCompanyLines()) {
                companyY = drawWrappedText(line, PAGE_MARGIN + 290f, companyY, 225f, 10.5f, PDType1Font.HELVETICA);
            }

            cursorY = Math.min(customerY, companyY) - 18f;
        }

        private void drawItemsTable() throws IOException {
            drawTableHeader();

            for (int index = 0; index < order.getItems().size(); index++) {
                OrderItem item = order.getItems().get(index);
                String productName = item.getProductSnapshot().getName();
                if (item.getVariantAttributes() != null && !item.getVariantAttributes().isEmpty()) {
                    String attributesStr = item.getVariantAttributes().entrySet().stream()
                        .map(e -> e.getKey() + ": " + e.getValue())
                        .collect(java.util.stream.Collectors.joining(", "));
                    productName += " (" + attributesStr + ")";
                }
                List<String> wrappedName = wrapText(productName, 250f, PDType1Font.HELVETICA, 10.5f);
                float rowHeight = Math.max(22f, wrappedName.size() * 14f + 8f);

                if (cursorY - rowHeight < PAGE_BOTTOM_MARGIN + 140f) {
                    startNewPage(true);
                    drawTableHeader();
                }

                double unitPrice = resolveItemUnitPrice(item);
                double lineTotal = unitPrice * item.getQuantity();
                float lineY = cursorY - 14f;

                drawText(String.valueOf(index + 1), PAGE_MARGIN + 5f, lineY, PDType1Font.HELVETICA, 10.5f);
                float itemTextY = cursorY - 8f;
                for (String line : wrappedName) {
                    drawText(line, PAGE_MARGIN + 40f, itemTextY, PDType1Font.HELVETICA, 10.5f);
                    itemTextY -= 14f;
                }
                drawText(String.valueOf(item.getQuantity()), PAGE_MARGIN + 320f, lineY, PDType1Font.HELVETICA, 10.5f);
                drawText(formatMoney(unitPrice), PAGE_MARGIN + 375f, lineY, PDType1Font.HELVETICA, 10.5f);
                drawText(formatMoney(lineTotal), PAGE_MARGIN + 470f, lineY, PDType1Font.HELVETICA, 10.5f);

                drawDivider(cursorY - rowHeight);
                cursorY -= rowHeight;
            }

            cursorY -= 20f;
        }

        private void drawTotalsBlock() throws IOException {
            float boxWidth = 210f;
            float boxHeight = 86f;
            float boxX = page.getMediaBox().getWidth() - PAGE_MARGIN - boxWidth;
            float boxY = cursorY - boxHeight;

            if (boxY < PAGE_BOTTOM_MARGIN + 90f) {
                startNewPage(true);
                boxY = cursorY - boxHeight;
            }

            drawBox(boxX, boxY, boxWidth, boxHeight);
            drawKeyValueRow(boxX, boxY + 60f, "Subtotal", formatMoney(totals.subtotal()));
            drawKeyValueRow(boxX, boxY + 42f, "Discount", "-" + formatMoney(totals.discount()));
            drawKeyValueRow(boxX, boxY + 24f, "Delivery", formatMoney(totals.deliveryCharge()));
            drawKeyValueRow(boxX, boxY + 6f, "Grand Total", formatMoney(totals.grandTotal()), true);

            cursorY = boxY - 16f;
        }

        private void drawFooter() throws IOException {
            if (cursorY < PAGE_BOTTOM_MARGIN + 70f) {
                startNewPage(true);
            }

            if (company.termsAndConditions() != null && !company.termsAndConditions().isBlank()) {
                drawText("Terms & Conditions", PAGE_MARGIN, cursorY, PDType1Font.HELVETICA_BOLD, 11f);
                cursorY = drawWrappedText(company.termsAndConditions(), PAGE_MARGIN, cursorY - 16f, 520f, 10f, PDType1Font.HELVETICA);
                cursorY -= 12f;
            }

            if (company.workingHours() != null && !company.workingHours().isBlank()) {
                drawText("Working Hours: " + company.workingHours(), PAGE_MARGIN, cursorY, PDType1Font.HELVETICA, 10f);
                cursorY -= 14f;
            }

            drawDivider(cursorY - 8f);
            drawText("This is a system-generated receipt for your order.", PAGE_MARGIN, cursorY - 22f, PDType1Font.HELVETICA_OBLIQUE, 9.5f);
        }

        private void drawTableHeader() throws IOException {
            drawBox(PAGE_MARGIN, cursorY - 20f, page.getMediaBox().getWidth() - (PAGE_MARGIN * 2), 20f);
            drawText("#", PAGE_MARGIN + 5f, cursorY - 14f, PDType1Font.HELVETICA_BOLD, 10f);
            drawText("Item", PAGE_MARGIN + 40f, cursorY - 14f, PDType1Font.HELVETICA_BOLD, 10f);
            drawText("Qty", PAGE_MARGIN + 320f, cursorY - 14f, PDType1Font.HELVETICA_BOLD, 10f);
            drawText("Unit Price", PAGE_MARGIN + 375f, cursorY - 14f, PDType1Font.HELVETICA_BOLD, 10f);
            drawText("Amount", PAGE_MARGIN + 470f, cursorY - 14f, PDType1Font.HELVETICA_BOLD, 10f);
            cursorY -= 26f;
            drawDivider(cursorY);
        }

        private void drawKeyValueRow(float boxX, float y, String key, String value) throws IOException {
            drawKeyValueRow(boxX, y, key, value, false);
        }

        private void drawKeyValueRow(float boxX, float y, String key, String value, boolean bold) throws IOException {
            drawText(key, boxX + 12f, y, bold ? PDType1Font.HELVETICA_BOLD : PDType1Font.HELVETICA, 10.5f);
            drawText(value, boxX + 120f, y, bold ? PDType1Font.HELVETICA_BOLD : PDType1Font.HELVETICA, 10.5f);
        }

        private String formatOrderDate() {
            return order.getCreatedAt() != null ? order.getCreatedAt().format(dateFormatter) : "";
        }

        private List<String> buildCustomerAddressLines(AddressSnapshot address) {
            List<String> lines = new ArrayList<>();
            if (address == null) {
                lines.add("Address details unavailable");
                return lines;
            }

            lines.add(defaultIfBlank(address.getFullName(), ""));
            lines.add(defaultIfBlank(address.getStreet(), ""));
            lines.add(joinNonBlank(address.getCity(), address.getPincode()));
            lines.add(defaultIfBlank(address.getCountry(), ""));
            if (address.getPhoneNumber() != null && !address.getPhoneNumber().isBlank()) {
                lines.add("Phone: " + address.getPhoneNumber());
            }
            lines.removeIf(String::isBlank);
            return lines;
        }

        private List<String> buildCompanyLines() {
            List<String> lines = new ArrayList<>();
            if (company.email() != null && !company.email().isBlank()) {
                lines.add("Email: " + company.email());
            }
            if (company.phone() != null && !company.phone().isBlank()) {
                lines.add("Phone: " + company.phone());
            }
            if (company.workingHours() != null && !company.workingHours().isBlank()) {
                lines.add("Hours: " + company.workingHours());
            }
            return lines;
        }

        private byte[] loadLogoBytes() {
            if (company.logoUrl() == null || company.logoUrl().isBlank()) {
                return null;
            }
            try {
                return fileUploadService.downloadFile(company.logoUrl());
            } catch (RuntimeException ex) {
                return null;
            }
        }

        private void drawText(String text, float x, float y, PDType1Font font, float fontSize) throws IOException {
            if (text == null || text.isBlank()) {
                return;
            }
            contentStream.beginText();
            contentStream.setFont(font, fontSize);
            contentStream.newLineAtOffset(x, y);
            contentStream.showText(text);
            contentStream.endText();
        }

        private float drawWrappedText(String text, float x, float y, float width, float fontSize, PDType1Font font) throws IOException {
            if (text == null || text.isBlank()) {
                return y;
            }

            List<String> lines = wrapText(text, width, font, fontSize);
            float currentY = y;
            for (String line : lines) {
                drawText(line, x, currentY, font, fontSize);
                currentY -= fontSize + 3f;
            }
            return currentY;
        }

        private List<String> wrapText(String text, float width, PDType1Font font, float fontSize) throws IOException {
            List<String> lines = new ArrayList<>();
            String[] words = text.split("\\s+");
            StringBuilder currentLine = new StringBuilder();

            for (String word : words) {
                String candidate = currentLine.length() == 0 ? word : currentLine + " " + word;
                float candidateWidth = font.getStringWidth(candidate) / 1000f * fontSize;
                if (candidateWidth > width && currentLine.length() > 0) {
                    lines.add(currentLine.toString());
                    currentLine = new StringBuilder(word);
                } else {
                    currentLine = new StringBuilder(candidate);
                }
            }

            if (currentLine.length() > 0) {
                lines.add(currentLine.toString());
            }
            return lines;
        }

        private void drawDivider(float y) throws IOException {
            contentStream.moveTo(PAGE_MARGIN, y);
            contentStream.lineTo(page.getMediaBox().getWidth() - PAGE_MARGIN, y);
            contentStream.stroke();
        }

        private void drawBox(float x, float y, float width, float height) throws IOException {
            contentStream.addRect(x, y, width, height);
            contentStream.stroke();
        }

        private String joinNonBlank(String first, String second) {
            String cleanFirst = clean(first);
            String cleanSecond = clean(second);
            if (cleanFirst == null || cleanFirst.isBlank()) {
                return cleanSecond == null ? "" : cleanSecond;
            }
            if (cleanSecond == null || cleanSecond.isBlank()) {
                return cleanFirst;
            }
            return cleanFirst + " | " + cleanSecond;
        }
    }

    private record ReceiptCompanyDetails(
        String companyName,
        String email,
        String phone,
        String address,
        String workingHours,
        String termsAndConditions,
        String logoUrl
    ) {}

    private record ReceiptTotals(
        double subtotal,
        double deliveryCharge,
        double discount,
        double grandTotal
    ) {}
}
