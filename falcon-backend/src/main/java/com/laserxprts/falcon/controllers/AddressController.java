package com.laserxprts.falcon.controllers;


import java.security.Principal;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.laserxprts.falcon.dto.request.AddressRequest;
import com.laserxprts.falcon.model.Address;
import com.laserxprts.falcon.service.AddressService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/address")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;

    @PostMapping
    public ResponseEntity<Address> addAddress(@RequestBody AddressRequest address, Principal principal) {
        Address savedAddress = addressService.addAddress(address, principal.getName());
        return ResponseEntity.ok(savedAddress);
    }

    @GetMapping
    public ResponseEntity<List<Address>> getUserAddresses(Principal principal) {
        List<Address> addresses = addressService.getAddresses(principal.getName());
        return ResponseEntity.ok(addresses);
    }

    // 3. Update Address
    @PutMapping("/{id}")
    public ResponseEntity<Address> updateAddress(
            @PathVariable String id,
            @RequestBody AddressRequest addressRequest,
            Principal principal) {

        Address updated = addressService.updateAddress(id, addressRequest, principal.getName());
        return ResponseEntity.ok(updated);
    }

    // 4. Delete Address
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteAddress(@PathVariable String id, Principal principal) {
        addressService.deleteAddress(id, principal.getName());
        return ResponseEntity.ok("Address deleted successfully");
    }
}