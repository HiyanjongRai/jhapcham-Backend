package com.example.jhapcham.delivery.application;


import com.example.jhapcham.delivery.application.*;
import com.example.jhapcham.delivery.domain.*;
import com.example.jhapcham.delivery.dto.*;
import com.example.jhapcham.delivery.persistence.*;
import com.example.jhapcham.Error.BusinessValidationException;
import com.example.jhapcham.Error.ResourceNotFoundException;
import com.example.jhapcham.order.domain.Order;
import com.example.jhapcham.order.persistence.OrderRepository;
import com.example.jhapcham.order.application.OrderService;
import com.example.jhapcham.order.domain.OrderStatus;
import com.example.jhapcham.order.domain.PaymentMethod;
import com.example.jhapcham.order.domain.PaymentStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrackingService {

    private static final SecureRandom OTP_RANDOM = new SecureRandom();

    private final ShipmentRepository shipmentRepository;
    private final DeliveryRepository deliveryRepository;
    private final TrackingRepository trackingRepository;
    private final OrderRepository orderRepository;
    private final OrderService orderService;
    private final CourierService courierService;
    private final CourierSettlementService settlementService;
    private final com.example.jhapcham.notification.application.EmailService emailService;

    @Transactional(readOnly = true)
    public TrackingResponseDTO getTracking(String trackingId) {
        Shipment shipment = shipmentRepository.findByTrackingId(trackingId)
                .orElseThrow(() -> new ResourceNotFoundException("Tracking ID not found"));

        return toTrackingResponse(shipment);
    }

    @Transactional
    public TrackingResponseDTO updateStatus(String trackingId, DeliveryStatus status, String location, String note, String otp) {
        Shipment shipment = shipmentRepository.findByTrackingId(trackingId)
                .orElseThrow(() -> new ResourceNotFoundException("Tracking ID not found"));

        validateForwardProgress(shipment.getStatus(), status);

        if (status == DeliveryStatus.DELIVERED
                && shipment.isCashOnDelivery()
                && (otp == null || otp.isBlank())) {
            throw new BusinessValidationException("COD delivery requires OTP and cash collection confirmation.");
        }

        if (status == DeliveryStatus.DELIVERED) {
            Order order = shipment.getOrder();
            if (otp == null || otp.isBlank()) {
                throw new BusinessValidationException("Delivery OTP is required to mark a shipment delivered.");
            }
            if (order.getDeliveryOtp() == null || !order.getDeliveryOtp().equals(otp)) {
                throw new BusinessValidationException("Invalid delivery OTP. Please ask the customer for the OTP.");
            }
            if (order.getDeliveryOtpExpiry() != null && LocalDateTime.now().isAfter(order.getDeliveryOtpExpiry())) {
                throw new BusinessValidationException("Delivery OTP has expired.");
            }
        }

        // AUTO-GENERATE OTP if it doesn't exist and we are starting delivery
        if (List.of(DeliveryStatus.RIDER_ASSIGNED, DeliveryStatus.PICKED_UP, DeliveryStatus.OUT_FOR_DELIVERY).contains(status)) {
            Order order = shipment.getOrder();
            if (order.getDeliveryOtp() == null) {
                String newOtp = String.format("%06d", OTP_RANDOM.nextInt(1_000_000));
                order.setDeliveryOtp(newOtp);
                order.setDeliveryOtpExpiry(LocalDateTime.now().plusMinutes(10));
                orderRepository.save(order);
                
                try {
                    emailService.sendDeliveryOtpEmail(order.getCustomerEmail(), order.getCustomerName(), newOtp);
                    log.info("OTP generated and sent for shipment {} as it transitioned to {}", trackingId, status);
                } catch (Exception e) {
                    log.error("Failed to send delivery OTP email for shipment {}: {}", trackingId, e.getMessage());
                }
            }
        }

        shipment.setStatus(status);
        if (status == DeliveryStatus.OUT_FOR_DELIVERY && shipment.getAssignedAt() == null) {
            shipment.setAssignedAt(LocalDateTime.now());
        }
        if (status == DeliveryStatus.DELIVERED) {
            shipment.setDeliveredAt(LocalDateTime.now());
            // Record COD cash in hand for courier (idempotent - safe to call always)
            settlementService.recordCodCollection(shipment);
            Order order = shipment.getOrder();
            if (order.getPaymentMethod() == PaymentMethod.COD) {
                order.setPaymentStatus(PaymentStatus.COD_COLLECTED);
            }
        }
        shipmentRepository.save(shipment);

        Delivery delivery = deliveryRepository.findTopByShipment_IdOrderByCreatedAtDesc(shipment.getId())
                .orElse(null);
        if (delivery != null) {
            delivery.setStatus(status);
            if (status == DeliveryStatus.PICKED_UP) {
                delivery.setPickedUpAt(LocalDateTime.now());
            }
            if (status == DeliveryStatus.DELIVERED) {
                delivery.setDeliveredAt(LocalDateTime.now());
            }
            deliveryRepository.save(delivery);
        }

        TrackingHistory history = TrackingHistory.builder()
                .shipment(shipment)
                .delivery(delivery)
                .status(status)
                .location(location)
                .note(note != null && !note.isBlank() ? note : defaultNote(status))
                .build();
        trackingRepository.save(history);

        syncOrderStatusFromDelivery(shipment.getOrder(), status);
        return toTrackingResponse(shipment);
    }

    @Transactional
    public TrackingResponseDTO deliverCod(String trackingId, String otp, BigDecimal collectedAmount, String location, String note) {
        Shipment shipment = shipmentRepository.findByTrackingId(trackingId)
                .orElseThrow(() -> new ResourceNotFoundException("Tracking ID not found"));

        if (!shipment.isCashOnDelivery()) {
            throw new BusinessValidationException("Shipment is not a cash on delivery order.");
        }
        if (collectedAmount == null) {
            throw new BusinessValidationException("Collected COD amount is required.");
        }

        BigDecimal expectedAmount = shipment.getCodAmount() != null ? shipment.getCodAmount() : BigDecimal.ZERO;
        if (collectedAmount.compareTo(expectedAmount) != 0) {
            throw new BusinessValidationException("Collected amount does not match the COD amount.");
        }

        return updateStatus(
                trackingId,
                DeliveryStatus.DELIVERED,
                location,
                note != null && !note.isBlank() ? note : "COD collected and delivery OTP verified",
                otp);
    }

    @Transactional(readOnly = true)
    public Shipment getShipmentByTrackingId(String trackingId) {
        return shipmentRepository.findByTrackingId(trackingId)
                .orElseThrow(() -> new ResourceNotFoundException("Tracking ID not found"));
    }

    @Transactional
    public void appendTracking(Shipment shipment, Delivery delivery, DeliveryStatus status, String location, String note) {
        TrackingHistory history = TrackingHistory.builder()
                .shipment(shipment)
                .delivery(delivery)
                .status(status)
                .location(location)
                .note(note)
                .build();
        trackingRepository.save(history);
    }

    public void syncOrderStatusFromDelivery(Order order, DeliveryStatus status) {
        OrderStatus mappedStatus = switch (status) {
            case CREATED, RIDER_ASSIGNED -> OrderStatus.PACKED;
            case PICKED_UP, IN_TRANSIT, DELAYED, CALL_NOT_PICKED, ADDRESS_NOT_FOUND -> OrderStatus.SHIPPED;
            case OUT_FOR_DELIVERY -> OrderStatus.OUT_FOR_DELIVERY;
            case DELIVERED -> OrderStatus.DELIVERED;
            case RETURN_TO_SELLER -> OrderStatus.RETURNED;
            case FAILED_DELIVERY -> OrderStatus.FAILED;
            case CANCELLED -> OrderStatus.CANCELLED;
        };

        if (order.getStatus() == mappedStatus) {
            return;
        }

        if (isRegressiveSync(order.getStatus(), mappedStatus)) {
            log.info("Skipping regressive delivery sync for order {}: {} -> {}", order.getId(), order.getStatus(), mappedStatus);
            return;
        }

        orderService.applySystemOrderStatusChange(order, mappedStatus, "Synced from delivery status " + status);
    }

    private boolean isRegressiveSync(OrderStatus current, OrderStatus mapped) {
        return switch (mapped) {
            case PACKED -> current == OrderStatus.SHIPPED
                    || current == OrderStatus.OUT_FOR_DELIVERY
                    || current == OrderStatus.DELIVERED
                    || current == OrderStatus.RETURNED
                    || current == OrderStatus.CANCELLED
                    || current == OrderStatus.FAILED;
            case SHIPPED -> current == OrderStatus.OUT_FOR_DELIVERY
                    || current == OrderStatus.DELIVERED
                    || current == OrderStatus.RETURNED
                    || current == OrderStatus.CANCELLED
                    || current == OrderStatus.FAILED;
            case OUT_FOR_DELIVERY -> current == OrderStatus.DELIVERED
                    || current == OrderStatus.RETURNED
                    || current == OrderStatus.CANCELLED
                    || current == OrderStatus.FAILED;
            default -> false;
        };
    }

    private void validateForwardProgress(DeliveryStatus current, DeliveryStatus next) {
        if (current == next) {
            return;
        }
        boolean valid = switch (current) {
            case CREATED -> List.of(
                    DeliveryStatus.RIDER_ASSIGNED,
                    DeliveryStatus.PICKED_UP,
                    DeliveryStatus.CANCELLED).contains(next);
            case RIDER_ASSIGNED -> List.of(
                    DeliveryStatus.PICKED_UP,
                    DeliveryStatus.CALL_NOT_PICKED,
                    DeliveryStatus.ADDRESS_NOT_FOUND,
                    DeliveryStatus.DELAYED,
                    DeliveryStatus.CANCELLED).contains(next);
            case PICKED_UP -> List.of(
                    DeliveryStatus.IN_TRANSIT,
                    DeliveryStatus.DELAYED,
                    DeliveryStatus.FAILED_DELIVERY,
                    DeliveryStatus.RETURN_TO_SELLER).contains(next);
            case IN_TRANSIT -> List.of(
                    DeliveryStatus.OUT_FOR_DELIVERY,
                    DeliveryStatus.DELAYED,
                    DeliveryStatus.RETURN_TO_SELLER).contains(next);
            case OUT_FOR_DELIVERY -> List.of(
                    DeliveryStatus.DELIVERED,
                    DeliveryStatus.FAILED_DELIVERY,
                    DeliveryStatus.CALL_NOT_PICKED,
                    DeliveryStatus.ADDRESS_NOT_FOUND,
                    DeliveryStatus.RETURN_TO_SELLER,
                    DeliveryStatus.DELAYED).contains(next);
            case DELAYED -> List.of(
                    DeliveryStatus.IN_TRANSIT,
                    DeliveryStatus.OUT_FOR_DELIVERY,
                    DeliveryStatus.RETURN_TO_SELLER,
                    DeliveryStatus.FAILED_DELIVERY).contains(next);
            case CALL_NOT_PICKED, ADDRESS_NOT_FOUND -> List.of(
                    DeliveryStatus.OUT_FOR_DELIVERY,
                    DeliveryStatus.FAILED_DELIVERY,
                    DeliveryStatus.RETURN_TO_SELLER,
                    DeliveryStatus.DELAYED).contains(next);
            case FAILED_DELIVERY -> List.of(DeliveryStatus.RETURN_TO_SELLER, DeliveryStatus.CANCELLED).contains(next);
            case RETURN_TO_SELLER, DELIVERED, CANCELLED -> false;
        };

        if (!valid) {
            throw new BusinessValidationException("Invalid delivery status transition");
        }
    }

    private String defaultNote(DeliveryStatus status) {
        return switch (status) {
            case CREATED -> "Shipment created for the packed order";
            case RIDER_ASSIGNED -> "Courier has been assigned to the shipment";
            case PICKED_UP -> "Courier has picked up the parcel";
            case IN_TRANSIT -> "Parcel is moving through the courier network";
            case OUT_FOR_DELIVERY -> "Courier is on the way to the destination";
            case DELIVERED -> "Parcel delivered successfully";
            case FAILED_DELIVERY -> "Delivery attempt failed";
            case RETURN_TO_SELLER -> "Shipment is being returned to the seller";
            case DELAYED -> "Shipment is delayed in transit";
            case CANCELLED -> "Shipment was cancelled";
            case CALL_NOT_PICKED -> "Customer call could not be completed";
            case ADDRESS_NOT_FOUND -> "Courier could not locate the delivery address";
        };
    }

    TrackingResponseDTO toTrackingResponse(Shipment shipment) {
        List<TrackingResponseDTO.TrackingEventDTO> timeline = trackingRepository
                .findByShipment_IdOrderByCreatedAtAsc(shipment.getId())
                .stream()
                .map(history -> TrackingResponseDTO.TrackingEventDTO.builder()
                        .status(history.getStatus())
                        .location(history.getLocation())
                        .note(history.getNote())
                        .createdAt(history.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        return TrackingResponseDTO.builder()
                .trackingId(shipment.getTrackingId())
                .deliveryStatus(shipment.getStatus())
                .courier(shipment.getCourier() != null ? courierService.toDto(shipment.getCourier(), null) : null)
                .cashOnDelivery(shipment.isCashOnDelivery())
                .codAmount(shipment.getCodAmount())
                .estimatedDeliveryAt(shipment.getEstimatedDeliveryAt())
                .deliveredAt(shipment.getDeliveredAt())
                .timeline(timeline)
                .build();
    }

    @Transactional
    public void resendOtp(String trackingId) {
        Shipment shipment = shipmentRepository.findByTrackingId(trackingId)
                .orElseThrow(() -> new ResourceNotFoundException("Tracking ID not found"));
        
        Order order = shipment.getOrder();
        if (order.getDeliveryOtpResendCount() >= 5) { // Increased limit for better UX
            throw new BusinessValidationException("Maximum OTP resend attempts exceeded.");
        }

        String newOtp = String.format("%06d", OTP_RANDOM.nextInt(1_000_000));
        order.setDeliveryOtp(newOtp);
        order.setDeliveryOtpExpiry(LocalDateTime.now().plusMinutes(10));
        order.setDeliveryOtpResendCount(order.getDeliveryOtpResendCount() + 1);
        orderRepository.save(order);

        try {
            emailService.sendDeliveryOtpEmail(order.getCustomerEmail(), order.getCustomerName(), newOtp);
            log.info("OTP resent for shipment {}", trackingId);
        } catch (Exception e) {
            log.error("Failed to resend delivery OTP email: {}", e.getMessage());
        }
    }
}
