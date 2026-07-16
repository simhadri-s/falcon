package com.laserxprts.falcon.controllers;

import java.util.HashMap;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.laserxprts.falcon.model.DeliveryLocation;
import com.laserxprts.falcon.service.DeliveryLocationService;

@RestController
@RequestMapping("/api/delivery-location")
public class DeliveryLocationController {
    private final DeliveryLocationService locationService;

    public DeliveryLocationController(DeliveryLocationService locationService) {
        this.locationService = locationService;
    }

    @PreAuthorize("permitAll()")
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAll(
        @RequestParam(value = "search", required = false) String search,
        @RequestParam(value = "page", defaultValue = "1") int page,
        @RequestParam(value = "limit", defaultValue = "10") int limit,
        @RequestParam(value = "sortBy", required = false) String sortBy,
        @RequestParam(value = "sortDirection", required = false) String sortDirection
    ) {
        Page<DeliveryLocation> response = locationService.getAll(search, page, limit, sortBy, sortDirection);
        Map<String, Object> result = new HashMap<>();
        result.put("data", response.getContent());
        result.put("page", page);
        result.put("total", response.getTotalElements());
        result.put("pages", response.getTotalPages());

        return ResponseEntity.ok(result);
    }

    @PreAuthorize("permitAll()")
    @GetMapping(params = "pincode")
    public ResponseEntity<DeliveryLocation> getDeliveryLocationByPincode(
        @RequestParam String pincode
    ) {
        DeliveryLocation response = locationService.getDeliveryLocation(pincode);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("permitAll()")
    @GetMapping("/{id}")
    public ResponseEntity<DeliveryLocation> getDeliveryLocationById(
        @PathVariable String id
    ) {
        DeliveryLocation response = locationService.getDeliveryLocationById(id);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("@permissionService.hasAccess('MANAGE_DELIVERYLOCATIONS')")
    @PostMapping
    public ResponseEntity<DeliveryLocation> createLocation(@RequestBody DeliveryLocation deliveryLocation) {
        return new ResponseEntity<>(locationService.createLocation(deliveryLocation), HttpStatus.CREATED);
    }

    @PreAuthorize("@permissionService.hasAccess('MANAGE_DELIVERYLOCATIONS')")
    @PutMapping("/{id}")
    public ResponseEntity<DeliveryLocation> updateLocation(
        @PathVariable String id, 
        @RequestBody DeliveryLocation location
    ) {
        return ResponseEntity.ok(locationService.updateLocation(id, location));
    }

    @PreAuthorize("@permissionService.hasAccess('MANAGE_DELIVERYLOCATIONS')")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteDeliveryLocation(@PathVariable String id) {
        locationService.deleteDeliveryLocation(id);
        return ResponseEntity.noContent().build();
    }
}
