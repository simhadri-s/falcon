package com.laserxprts.falcon.service;

import com.laserxprts.falcon.dto.request.PriceRequest;
import com.laserxprts.falcon.model.Price;
import com.laserxprts.falcon.repository.PriceRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class PriceService {

    private final PriceRepository priceRepository;

    public PriceService(PriceRepository priceRepository) {
        this.priceRepository = priceRepository;
    }

    public List<PriceRequest> getAllPrices() {
        return priceRepository.findAll().stream().map(this::toDto).collect(Collectors.toList());
    }

    public PriceRequest getPriceById(String id) {
        return priceRepository.findById(id).map(this::toDto).orElse(null);
    }

    public PriceRequest createPrice(PriceRequest dto) {
        Price price = new Price();
        price.setPrice(dto.getAmount());
        price.setCurrency(dto.getCurrency());
        return toDto(priceRepository.save(price));
    }

    public PriceRequest updatePrice(String id, PriceRequest dto) {
        return priceRepository.findById(id).map(price -> {
            price.setPrice(dto.getAmount());
            price.setCurrency(dto.getCurrency());
            return toDto(priceRepository.save(price));
        }).orElse(null);
    }

     public boolean deletePrice(String id) {
        if (priceRepository.existsById(id)) {
            priceRepository.deleteById(id);
            return true;
        }
        return false;
    }



    private PriceRequest toDto(Price price) {
        PriceRequest dto = new PriceRequest();
        dto.setId(price.getId());
        dto.setAmount(price.getPrice());
        dto.setCurrency(price.getCurrency());
        dto.setCreatedAt(price.getCreatedAt());
        return dto;
    }
}
