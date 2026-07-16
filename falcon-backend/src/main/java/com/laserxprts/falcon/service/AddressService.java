package com.laserxprts.falcon.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.laserxprts.falcon.dto.request.AddressRequest;
import com.laserxprts.falcon.model.Address;
import com.laserxprts.falcon.model.User;
import com.laserxprts.falcon.repository.AddressRepository;
import com.laserxprts.falcon.repository.DeliveryLocationRepository;
import com.laserxprts.falcon.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AddressService {

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;
    private final DeliveryLocationRepository locationRepository;

    // ➤ Add Address
    public Address addAddress(AddressRequest request, String email) {
        validatePincode(request.getPincode());
        User user = getUser(email);

        if (request.isDefault()) {
            removeExistingDefault(user.getId());
        }

        Address savedAddress = mapToEntity(request, user.getId());
        return addressRepository.save(savedAddress);
    }

    // ➤ Get All
    public List<Address> getAddresses(String email) {
        User user = getUser(email);
        return addressRepository.findByUserId(user.getId());
    }

    // ➤ Get Default
    public Address getDefaultAddress(String email) {
        User user = getUser(email);
        return addressRepository
                .findByUserIdAndIsDefaultTrue(user.getId())
                .orElseThrow(() -> new RuntimeException("Default address not found"));
    }

    // ➤ Update
    public Address updateAddress(String id, AddressRequest request, String email) {
        validatePincode(request.getPincode());
        User user = getUser(email);

        Address address = addressRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Address not found"));

        // 🔐 Security check
        if (!address.getUserId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized");
        }

        // Update fields
        address.setFullName(request.getFullName());
        address.setPhoneNumber(request.getPhoneNumber());
        address.setStreet(request.getStreet());
        address.setCity(request.getCity());
        address.setState(request.getState());
        address.setPincode(request.getPincode());
        address.setCountry(request.getCountry());

        // Default logic
        if (request.isDefault()) {
            removeExistingDefault(user.getId());
            address.setDefault(true);
        }

        return addressRepository.save(address);
    }

    // ➤ Delete
    public void deleteAddress(String id, String email) {
        User user = getUser(email);

        Address address = addressRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Address not found"));

        if (!address.getUserId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized");
        }

        addressRepository.delete(address);
    }

    // 🔹 Helper: Remove old default
    private void removeExistingDefault(String userId) {
        List<Address> addresses = addressRepository.findByUserId(userId);
        addresses.forEach(a -> {
            if (a.isDefault()) {
                a.setDefault(false);
                addressRepository.save(a);
            }
        });
    }

    // 🔹 Helper: Get user
    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    // 🔹 Mapper
    private Address mapToEntity(AddressRequest request, String userId) {
        Address address = new Address();
        address.setFullName(request.getFullName());
        address.setPhoneNumber(request.getPhoneNumber());
        address.setStreet(request.getStreet());
        address.setCity(request.getCity());
        address.setState(request.getState());
        address.setPincode(request.getPincode());
        address.setCountry(request.getCountry());
        address.setDefault(request.isDefault());
        address.setUserId(userId);
        return address;
    }
     // Helper method for validation
    private void validatePincode(String pincode) {
        if (!locationRepository.existsByPincode(pincode)) {
            throw new IllegalArgumentException("Delivery not available to this pincode: " + pincode);
        }
    }
}
