package com.laserxprts.falcon.controllers;

import com.laserxprts.falcon.dto.request.PriceRequest;
import com.laserxprts.falcon.service.PriceService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/prices")
public class PriceController {

    private final PriceService priceService;

    public PriceController(PriceService priceService) {
        this.priceService = priceService;
    }

    @GetMapping
    public List<PriceRequest> getAllPrices() {
        return priceService.getAllPrices();
    }

    @GetMapping("/{id}")
    public PriceRequest getPriceById(@PathVariable String id) {
        return priceService.getPriceById(id);
    }

    @PostMapping
    public com.laserxprts.falcon.dto.request.PriceRequest createPrice(@RequestBody PriceRequest dto) {
        return priceService.createPrice(dto);
    }

    @PutMapping("/{id}")
    public PriceRequest updatePrice(@PathVariable String id, @RequestBody PriceRequest dto) {
        return priceService.updatePrice(id, dto);
    }

    @DeleteMapping("/{id}")
    public void deletePrice(@PathVariable String id) {
        priceService.deletePrice(id);
    }
}
