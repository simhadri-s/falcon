package com.laserxprts.falcon.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.laserxprts.falcon.model.DeliveryLocation;
import com.laserxprts.falcon.repository.DeliveryLocationRepository;

@Service
public class DeliveryLocationService {
    
    private final DeliveryLocationRepository locationRepository;

    public DeliveryLocationService(DeliveryLocationRepository locationRepository) {
        this.locationRepository = locationRepository;
    }

    public Page<DeliveryLocation> getAll(String search, int page, int limit, String sortBy, String sortDirection) {
        String sortField = (sortBy != null && !sortBy.isEmpty()) ? sortBy : "id";
        Sort.Direction direction = Sort.Direction.ASC;
        if (sortDirection != null && sortDirection.equalsIgnoreCase("desc")) {
            direction = Sort.Direction.DESC;
        }
        
        Pageable pageable = PageRequest.of(
            Math.max(page-1, 0),
            limit,
            Sort.by(direction, sortField)
        );

        if (search != null && !search.isBlank()){
            return locationRepository.search(search, pageable);
        }
        return locationRepository.findAll(pageable);
    }

    public DeliveryLocation getDeliveryLocation(String pincode) {
        if (pincode == null || pincode.isBlank()) {
            throw new RuntimeException("Pincode is empty");
        }
        return locationRepository.findByPincode(pincode.trim())
            .orElseThrow(() -> new RuntimeException("Cannot find delivery location"));
    }

    public DeliveryLocation createLocation(DeliveryLocation deliveryLocation) {
        if (deliveryLocation == null) {
            throw new RuntimeException("Delivery location is empty");
        }
        if (deliveryLocation.getPincode() == null || deliveryLocation.getPincode().isBlank()) {
            throw new RuntimeException("Pincode is empty");
        }

        if (deliveryLocation.getLocation() == null || deliveryLocation.getLocation().isBlank()) {
            throw new RuntimeException("Delivery location is empty");
        }
        return locationRepository.save(deliveryLocation);
    }

    public DeliveryLocation updateLocation(String id, DeliveryLocation deliveryLocation) {
        if (deliveryLocation == null) {
            throw new RuntimeException("Delivery location is empty");
        }
        DeliveryLocation updatedLocation = locationRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Cannot find the location"));

        if (deliveryLocation.getLocation() != null && !deliveryLocation.getLocation().isBlank()) {
            updatedLocation.setLocation(deliveryLocation.getLocation());
        }

        if (deliveryLocation.getPincode() != null && !deliveryLocation.getPincode().isBlank()) {
            updatedLocation.setPincode(deliveryLocation.getPincode());
        }
        
        updatedLocation.setDeliveryCharge(deliveryLocation.getDeliveryCharge());
        return locationRepository.save(updatedLocation);
    }

    public DeliveryLocation getDeliveryLocationById(String id) {
        if (id == null || id.isBlank()) {
            throw new RuntimeException("ID is empty");
        }
        return locationRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Cannot find the location"));
    }

    public void deleteDeliveryLocation(String id) {
        if (id == null || id.isBlank()) {
            throw new RuntimeException("ID is empty");
        }
        locationRepository.deleteById(id);
    }
}
