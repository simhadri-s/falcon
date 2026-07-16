package com.laserxprts.falcon.tasks;

import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.laserxprts.falcon.model.Product;
import com.laserxprts.falcon.repository.ProductRepository;
import com.laserxprts.falcon.service.ProductService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductOfferTask {

    private final ProductRepository productRepository;
    private final ProductService productService;

    /**
     * Runs daily at 1:00 AM to update product expiry offers.
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void runExpiryOfferCheck() {
        log.info("Starting automated product expiry offer check...");
        
        List<Product> productsToProcess = productRepository.findByAutoOfferOnExpiryTrueOrExpiryOfferTrue();
        int updatedCount = 0;

        for (Product product : productsToProcess) {
            if (product.isAutoOfferOnExpiry()) {
                boolean wasOnOffer = product.isExpiryOffer();
                Double oldPrice = product.getSellingPrice();
                
                productService.reevaluateExpiryOffer(product);
                
                if (wasOnOffer != product.isExpiryOffer() || (product.isExpiryOffer() && !oldPrice.equals(product.getSellingPrice()))) {
                    productRepository.save(product);
                    updatedCount++;
                }
            } else if (product.isExpiryOffer()) {
                // If auto-offer was disabled manually but flag is still true
                productService.reevaluateExpiryOffer(product);
                productRepository.save(product);
                updatedCount++;
            }
        }

        log.info("Completed automated product expiry offer check. Updated {} products.", updatedCount);
    }
}
