package com.example.jhapcham.delivery.api;


import com.example.jhapcham.delivery.application.*;
import com.example.jhapcham.delivery.domain.*;
import com.example.jhapcham.delivery.dto.*;
import com.example.jhapcham.delivery.persistence.*;
import com.example.jhapcham.Error.ErrorResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tracking")
@RequiredArgsConstructor
public class TrackingController {

    private final TrackingService trackingService;
    private final CurrentCourierService currentCourierService;

    @GetMapping("/{trackingId}")
    public ResponseEntity<?> getTracking(@PathVariable String trackingId) {
        try {
            return ResponseEntity.ok(trackingService.getTracking(trackingId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    @PutMapping({"/update", "/update-status"})
    public ResponseEntity<?> update(@RequestBody TrackingResponseDTO.UpdateRequest request,
                                    Authentication authentication) {
        try {
            Courier courier = currentCourierService.requireCourier(authentication);
            Shipment shipment = trackingService.getShipmentByTrackingId(request.getTrackingId());
            if (shipment.getCourier() == null || !shipment.getCourier().getId().equals(courier.getId())) {
                throw new com.example.jhapcham.Error.AuthorizationException("This shipment is not assigned to the logged-in courier");
            }

            if (request.getStatus() == DeliveryStatus.DELIVERED && (request.getOtp() == null || request.getOtp().isBlank())) {
                throw new com.example.jhapcham.Error.BusinessValidationException("OTP is required for delivery completion.");
            }

            if (request.getStatus() == DeliveryStatus.DELIVERED && shipment.isCashOnDelivery()) {
                return ResponseEntity.ok(
                        trackingService.deliverCod(
                                request.getTrackingId(),
                                request.getOtp(),
                                request.getCollectedAmount(),
                                request.getLocation(),
                                request.getNote()));
            }

            return ResponseEntity.ok(
                    trackingService.updateStatus(
                            request.getTrackingId(),
                            request.getStatus(),
                            request.getLocation(),
                            request.getNote(),
                            request.getOtp()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    @PostMapping("/{trackingId}/resend-otp")
    public ResponseEntity<?> resendOtp(@PathVariable String trackingId,
                                      Authentication authentication) {
        try {
            Courier courier = currentCourierService.requireCourier(authentication);
            Shipment shipment = trackingService.getShipmentByTrackingId(trackingId);
            
            if (shipment.getCourier() == null || !shipment.getCourier().getId().equals(courier.getId())) {
                throw new com.example.jhapcham.Error.AuthorizationException("This shipment is not assigned to the logged-in courier");
            }

            trackingService.resendOtp(trackingId);
            return ResponseEntity.ok(java.util.Map.of("message", "OTP resent successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }
}
