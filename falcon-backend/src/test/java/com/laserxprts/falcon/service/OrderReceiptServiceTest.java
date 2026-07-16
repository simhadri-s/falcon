package com.laserxprts.falcon.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudinary.Cloudinary;
import com.laserxprts.falcon.model.AddressSnapshot;
import com.laserxprts.falcon.model.CompanySettings;
import com.laserxprts.falcon.model.Order;
import com.laserxprts.falcon.model.OrderItem;
import com.laserxprts.falcon.model.ProductSnapshot;
import com.laserxprts.falcon.model.ReceiptDocument;
import com.laserxprts.falcon.repository.CompanyImageRepository;
import com.laserxprts.falcon.repository.CompanySettingsRepository;
import com.laserxprts.falcon.repository.OrderRepository;

@ExtendWith(MockitoExtension.class)
class OrderReceiptServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CompanySettingsRepository companySettingsRepository;

    @Mock
    private CompanyImageRepository companyImageRepository;

    @Test
    void ensureReceiptAvailableReturnsExistingReceiptWithoutUploading() {
        StubFileUploadService fileUploadService = new StubFileUploadService();
        OrderReceiptService orderReceiptService = new OrderReceiptService(
            orderRepository,
            companySettingsRepository,
            companyImageRepository,
            fileUploadService
        );
        Order order = buildOrder();
        order.setReceipt(ReceiptDocument.builder()
            .fileName("receipt-ORD1234567.pdf")
            .url("https://cdn.example/receipt.pdf")
            .publicId("falcon/receipts/receipt_ORD1234567.pdf")
            .build());

        Order result = orderReceiptService.ensureReceiptAvailable(order);

        assertSame(order, result);
        assertEquals(0, fileUploadService.uploadCalls);
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void ensureReceiptAvailableUploadsAndSavesReceiptMetadata() {
        StubFileUploadService fileUploadService = new StubFileUploadService();
        OrderReceiptService orderReceiptService = new OrderReceiptService(
            orderRepository,
            companySettingsRepository,
            companyImageRepository,
            fileUploadService
        );
        CompanySettings settings = new CompanySettings();
        settings.setId("COMPANY_SETTINGS");
        settings.setCompanyName("Falcon Store");
        settings.setEmail("support@falcon.test");
        settings.setPhone("+91-9999999999");
        settings.setAddress("Hosur");
        settings.setWorkingHours("9 AM - 6 PM");

        when(companySettingsRepository.findById("COMPANY_SETTINGS")).thenReturn(Optional.of(settings));
        when(companyImageRepository.findAll()).thenReturn(List.of());
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Order result = orderReceiptService.ensureReceiptAvailable(buildOrder());

        assertNotNull(result.getReceipt());
        assertEquals("receipt-ORD1234567.pdf", result.getReceipt().getFileName());
        assertEquals("falcon/receipts/receipt_ORD1234567.pdf", result.getReceipt().getPublicId());
        assertNotNull(result.getReceipt().getGeneratedAt());
        assertEquals(1, fileUploadService.uploadCalls);
        assertEquals("receipt-ORD1234567.pdf", fileUploadService.lastFileName);
        assertEquals("receipt_ORD1234567.pdf", fileUploadService.lastPublicId);
        assertNotNull(fileUploadService.lastUploadBytes);
        verify(orderRepository).save(any(Order.class));
    }

    private Order buildOrder() {
        ProductSnapshot snapshot = ProductSnapshot.builder()
            .id("PROD-1")
            .name("Laser Cutter Lens")
            .sellingPrice(1250.0)
            .mrp(1500.0)
            .build();

        OrderItem item = OrderItem.builder()
            .productSnapshot(snapshot)
            .quantity(2)
            .build();

        return Order.builder()
            .id("ORD1234567")
            .userId("customer@example.com")
            .items(List.of(item))
            .addressSnapshot(AddressSnapshot.builder()
                .fullName("Sample Customer")
                .street("12 Main Road")
                .city("Hosur")
                .pincode("635109")
                .country("India")
                .phoneNumber("9999999999")
                .build())
            .status("CREATED")
            .deliveryCharge(100.0)
            .discountAmount(50.0)
            .createdAt(LocalDateTime.of(2026, 5, 14, 10, 30))
            .build();
    }

    private static class StubFileUploadService extends FileUploadService {
        private int uploadCalls;
        private byte[] lastUploadBytes;
        private String lastFileName;
        private String lastPublicId;

        private StubFileUploadService() {
            super((Cloudinary) null);
        }

        @Override
        public UploadedAsset uploadReceiptPdf(byte[] fileBytes, String fileName, String publicId) {
            this.uploadCalls++;
            this.lastUploadBytes = fileBytes;
            this.lastFileName = fileName;
            this.lastPublicId = publicId;
            return new UploadedAsset(
                "https://cdn.example/receipt.pdf",
                "falcon/receipts/" + publicId,
                fileName
            );
        }

        @Override
        public byte[] downloadFile(String fileUrl) {
            return new byte[] { 1, 2, 3 };
        }
    }
}
