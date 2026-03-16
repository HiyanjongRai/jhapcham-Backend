package com.example.jhapcham.user.model;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/addresses")
@RequiredArgsConstructor
public class AddressController {

    private final UserAddressRepository addressRepository;

    @GetMapping("/{userId}")
    public ResponseEntity<List<UserAddress>> getAddresses(@PathVariable Long userId) {
        return ResponseEntity.ok(addressRepository.findByUserIdOrderByIsDefaultDesc(userId));
    }

    @PostMapping("/{userId}")
    public ResponseEntity<UserAddress> addAddress(@PathVariable Long userId, @RequestBody Map<String, Object> body) {
        UserAddress address = UserAddress.builder()
                .userId(userId)
                .label((String) body.getOrDefault("label", "Home"))
                .receiverName((String) body.get("receiverName"))
                .receiverPhone((String) body.get("receiverPhone"))
                .city((String) body.get("city"))
                .state((String) body.get("state"))
                .street((String) body.get("street"))
                .landMark((String) body.get("landMark"))
                .fullAddress((String) body.get("fullAddress"))
                .isDefault(Boolean.TRUE.equals(body.get("isDefault")))
                .build();

        // If this is set as default, un-default the others
        if (Boolean.TRUE.equals(address.getIsDefault())) {
            addressRepository.findByUserIdOrderByIsDefaultDesc(userId).forEach(a -> {
                if (Boolean.TRUE.equals(a.getIsDefault())) {
                    a.setIsDefault(false);
                    addressRepository.save(a);
                }
            });
        }

        return ResponseEntity.ok(addressRepository.save(address));
    }

    @PutMapping("/{addressId}")
    public ResponseEntity<UserAddress> updateAddress(@PathVariable Long addressId, @RequestBody Map<String, Object> body) {
        UserAddress address = addressRepository.findById(addressId)
                .orElseThrow(() -> new RuntimeException("Address not found"));

        if (body.containsKey("label")) address.setLabel((String) body.get("label"));
        if (body.containsKey("receiverName")) address.setReceiverName((String) body.get("receiverName"));
        if (body.containsKey("receiverPhone")) address.setReceiverPhone((String) body.get("receiverPhone"));
        if (body.containsKey("city")) address.setCity((String) body.get("city"));
        if (body.containsKey("state")) address.setState((String) body.get("state"));
        if (body.containsKey("street")) address.setStreet((String) body.get("street"));
        if (body.containsKey("landMark")) address.setLandMark((String) body.get("landMark"));
        if (body.containsKey("fullAddress")) address.setFullAddress((String) body.get("fullAddress"));
        if (body.containsKey("isDefault")) {
            boolean setDefault = Boolean.TRUE.equals(body.get("isDefault"));
            if (setDefault) {
                addressRepository.findByUserIdOrderByIsDefaultDesc(address.getUserId()).forEach(a -> {
                    if (!a.getId().equals(addressId) && Boolean.TRUE.equals(a.getIsDefault())) {
                        a.setIsDefault(false);
                        addressRepository.save(a);
                    }
                });
            }
            address.setIsDefault(setDefault);
        }

        return ResponseEntity.ok(addressRepository.save(address));
    }

    @DeleteMapping("/{addressId}")
    public ResponseEntity<Void> deleteAddress(@PathVariable Long addressId) {
        addressRepository.deleteById(addressId);
        return ResponseEntity.ok().build();
    }
}
