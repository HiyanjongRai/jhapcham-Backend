package com.example.jhapcham.delivery.api;


import com.example.jhapcham.delivery.application.*;
import com.example.jhapcham.delivery.domain.*;
import com.example.jhapcham.delivery.dto.*;
import com.example.jhapcham.delivery.persistence.*;
import com.example.jhapcham.Error.ErrorResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/courier")
@RequiredArgsConstructor
public class CourierController {

    private final CourierService courierService;
    private final CurrentCourierService currentCourierService;
    private final ShipmentService shipmentService;

    @PostMapping("/create")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> create(@RequestBody CourierDTO dto) {
        try {
            return ResponseEntity.ok(courierService.createCourier(dto));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> all() {
        return ResponseEntity.ok(courierService.getAllCouriers());
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody CourierDTO dto) {
        try {
            return ResponseEntity.ok(courierService.login(dto));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    @GetMapping("/assigned")
    public ResponseEntity<?> assigned(Authentication authentication) {
        try {
            Courier courier = currentCourierService.requireCourier(authentication);
            shipmentService.repairMissingShipmentsForDeliveryStageOrders(courier);
            return ResponseEntity.ok(courierService.getAssignedShipments(courier.getId()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }
}
