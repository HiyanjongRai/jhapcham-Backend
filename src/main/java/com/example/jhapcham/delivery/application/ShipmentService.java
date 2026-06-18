package com.example.jhapcham.delivery.application;


import com.example.jhapcham.delivery.application.*;
import com.example.jhapcham.delivery.domain.*;
import com.example.jhapcham.delivery.dto.*;
import com.example.jhapcham.delivery.persistence.*;
import com.example.jhapcham.Error.BusinessValidationException;
import com.example.jhapcham.Error.ResourceNotFoundException;
import com.example.jhapcham.order.domain.Order;
import com.example.jhapcham.order.persistence.OrderRepository;
import com.example.jhapcham.order.domain.OrderStatus;
import com.example.jhapcham.order.domain.PaymentMethod;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ShipmentService {

    private final ShipmentRepository shipmentRepository;
    private final OrderRepository orderRepository;
    private final DeliveryRepository deliveryRepository;
    private final CourierService courierService;
    private final TrackingService trackingService;

    @Transactional
    public DeliveryResponseDTO createShipment(Long orderId) {
        Shipment existing = shipmentRepository.findByOrder_Id(orderId).orElse(null);
        if (existing != null) {
            return toDeliveryResponse(existing);
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        if (!List.of(
                com.example.jhapcham.order.domain.OrderStatus.PACKED,
                com.example.jhapcham.order.domain.OrderStatus.SHIPPED,
                com.example.jhapcham.order.domain.OrderStatus.OUT_FOR_DELIVERY).contains(order.getStatus())) {
            throw new BusinessValidationException("Shipment can only be created for packed, shipped, or out-for-delivery orders");
        }
        if (order.getPaymentMethod() != PaymentMethod.COD
                && order.getPaymentStatus() != com.example.jhapcham.order.domain.PaymentStatus.PAID) {
            throw new BusinessValidationException("Prepaid orders must be paid before shipment");
        }

        Shipment shipment = Shipment.builder()
                .trackingId(generateTrackingId())
                .order(order)
                .status(DeliveryStatus.CREATED)
                .destinationAddress(order.getShippingAddress())
                .shippingLocation(order.getShippingLocation())
                .cashOnDelivery(order.getPaymentMethod() == PaymentMethod.COD)
                .codAmount(order.getGrandTotal() != null ? order.getGrandTotal() : BigDecimal.ZERO)
                .estimatedDeliveryAt(estimateDelivery(order.getShippingLocation()))
                .notes("Shipment created for packed order")
                .build();

        Courier randomCourier = courierService.getRandomActiveCourier();
        if (randomCourier != null) {
            shipment.setCourier(randomCourier);
            shipment.setAssignedAt(LocalDateTime.now());
            shipment.setNotes("Courier assigned automatically");
        }

        Shipment savedShipment = shipmentRepository.save(shipment);

        Delivery delivery = Delivery.builder()
                .shipment(savedShipment)
                .courier(savedShipment.getCourier())
                .status(savedShipment.getStatus())
                .notes(savedShipment.getNotes())
                .attemptCount(0)
                .assignedAt(savedShipment.getAssignedAt())
                .build();
        deliveryRepository.save(delivery);
        savedShipment.getDeliveries().add(delivery);

        trackingService.appendTracking(
                savedShipment,
                delivery,
                DeliveryStatus.CREATED,
                "Warehouse",
                "Shipment created and ready for courier dispatch");

        if (savedShipment.getCourier() != null) {
            trackingService.appendTracking(
                    savedShipment,
                    delivery,
                    DeliveryStatus.RIDER_ASSIGNED,
                    savedShipment.getCourier().getCurrentDistrict(),
                    "Courier " + savedShipment.getCourier().getFullName() + " assigned automatically");
        }

        if (!List.of(
                com.example.jhapcham.order.domain.OrderStatus.SHIPPED,
                com.example.jhapcham.order.domain.OrderStatus.OUT_FOR_DELIVERY,
                com.example.jhapcham.order.domain.OrderStatus.DELIVERED).contains(order.getStatus())) {
            trackingService.syncOrderStatusFromDelivery(order, DeliveryStatus.CREATED);
        }

        return toDeliveryResponse(savedShipment);
    }

    @Transactional
    public DeliveryResponseDTO assignCourier(Long shipmentId) {
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment not found"));

        Courier courier = courierService.getRandomActiveCourier();
        if (courier == null) {
            throw new BusinessValidationException("No active couriers available");
        }

        shipment.setCourier(courier);
        shipment.setAssignedAt(LocalDateTime.now());
        if (shipment.getStatus() == DeliveryStatus.CREATED) {
            shipment.setStatus(DeliveryStatus.RIDER_ASSIGNED);
        }
        shipmentRepository.save(shipment);

        Delivery delivery = deliveryRepository.findTopByShipment_IdOrderByCreatedAtDesc(shipmentId)
                .orElseGet(() -> Delivery.builder().shipment(shipment).build());
        delivery.setCourier(courier);
        delivery.setStatus(shipment.getStatus());
        delivery.setAssignedAt(LocalDateTime.now());
        delivery.setNotes("Courier assigned");
        deliveryRepository.save(delivery);
        if (!shipment.getDeliveries().contains(delivery)) {
            shipment.getDeliveries().add(delivery);
        }

        trackingService.appendTracking(
                shipment,
                delivery,
                DeliveryStatus.RIDER_ASSIGNED,
                courier.getCurrentDistrict(),
                "Courier " + courier.getFullName() + " assigned");

        trackingService.syncOrderStatusFromDelivery(shipment.getOrder(), shipment.getStatus());

        return toDeliveryResponse(shipment);
    }

    @Transactional
    public DeliveryResponseDTO cancelShipmentForOrder(Long orderId, String note) {
        Shipment shipment = shipmentRepository.findByOrder_Id(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment not found for order"));

        if (shipment.getStatus() == DeliveryStatus.DELIVERED || shipment.getStatus() == DeliveryStatus.RETURN_TO_SELLER) {
            return toDeliveryResponse(shipment);
        }

        shipment.setStatus(DeliveryStatus.CANCELLED);
        shipment.setNotes(note);
        shipmentRepository.save(shipment);

        Delivery delivery = deliveryRepository.findTopByShipment_IdOrderByCreatedAtDesc(shipment.getId())
                .orElse(null);
        if (delivery != null) {
            delivery.setStatus(DeliveryStatus.CANCELLED);
            delivery.setNotes(note);
            deliveryRepository.save(delivery);
        }

        trackingService.appendTracking(
                shipment,
                delivery,
                DeliveryStatus.CANCELLED,
                shipment.getCourier() != null ? shipment.getCourier().getCurrentDistrict() : "Warehouse",
                note != null && !note.isBlank() ? note : "Shipment cancelled");
        trackingService.syncOrderStatusFromDelivery(shipment.getOrder(), DeliveryStatus.CANCELLED);

        return toDeliveryResponse(shipment);
    }

    @Transactional(readOnly = true)
    public DeliveryResponseDTO getShipment(Long shipmentId) {
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment not found"));
        return toDeliveryResponse(shipment);
    }

    @Transactional(readOnly = true)
    public Shipment getShipmentEntity(Long shipmentId) {
        return shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment not found"));
    }

    @Transactional(readOnly = true)
    public Shipment findByOrderId(Long orderId) {
        return shipmentRepository.findByOrder_Id(orderId).orElse(null);
    }

    @Transactional
    public void syncShipmentForOrderStatus(Order order, String note) {
        if (order == null || order.getId() == null) {
            return;
        }

        switch (order.getStatus()) {
            case PACKED -> {
                createShipment(order.getId());
                ensureCourierAssigned(order.getId());
            }
            case SHIPPED -> advanceShipmentForOrder(order.getId(), DeliveryStatus.PICKED_UP, note);
            case OUT_FOR_DELIVERY -> advanceShipmentForOrder(order.getId(), DeliveryStatus.OUT_FOR_DELIVERY, note);
            case CANCELLED, FAILED -> {
                Shipment shipment = findByOrderId(order.getId());
                if (shipment != null && shipment.getStatus() != DeliveryStatus.CANCELLED
                        && shipment.getStatus() != DeliveryStatus.DELIVERED
                        && shipment.getStatus() != DeliveryStatus.RETURN_TO_SELLER) {
                    cancelShipmentForOrder(order.getId(), note != null ? note : "Order cancelled");
                }
            }
            case RETURNED -> {
                Shipment shipment = findByOrderId(order.getId());
                if (shipment != null && shipment.getStatus() != DeliveryStatus.RETURN_TO_SELLER) {
                    trackingService.updateStatus(
                            shipment.getTrackingId(),
                            DeliveryStatus.RETURN_TO_SELLER,
                            shipment.getShippingLocation(),
                            note != null ? note : "Order returned to seller",
                            null);
                }
            }
            default -> {
            }
        }
    }

    @Transactional
    public void repairMissingShipmentsForDeliveryStageOrders() {
        repairMissingShipmentsForDeliveryStageOrders(null);
    }

    @Transactional
    public void repairMissingShipmentsForDeliveryStageOrders(Courier preferredCourier) {
        List<Order> missingShipmentOrders = orderRepository.findDeliveryStageOrdersWithoutShipment(List.of(
                OrderStatus.PACKED,
                OrderStatus.SHIPPED,
                OrderStatus.OUT_FOR_DELIVERY));

        for (Order order : missingShipmentOrders) {
            syncShipmentForOrderStatus(order, "Recovered missing courier shipment");
            assignRecoveredShipmentToCourier(order.getId(), preferredCourier);
        }
    }

    private void assignRecoveredShipmentToCourier(Long orderId, Courier preferredCourier) {
        if (preferredCourier == null) {
            return;
        }

        Shipment shipment = findByOrderId(orderId);
        if (shipment == null) {
            return;
        }

        shipment.setCourier(preferredCourier);
        if (shipment.getAssignedAt() == null) {
            shipment.setAssignedAt(LocalDateTime.now());
        }
        shipmentRepository.save(shipment);

        Delivery delivery = deliveryRepository.findTopByShipment_IdOrderByCreatedAtDesc(shipment.getId())
                .orElse(null);
        if (delivery != null) {
            delivery.setCourier(preferredCourier);
            if (delivery.getAssignedAt() == null) {
                delivery.setAssignedAt(LocalDateTime.now());
            }
            deliveryRepository.save(delivery);
        }
    }

    private void advanceShipmentForOrder(Long orderId, DeliveryStatus targetStatus, String note) {
        Shipment shipment = findByOrderId(orderId);
        if (shipment == null) {
            createShipment(orderId);
            shipment = findByOrderId(orderId);
        }
        ensureCourierAssigned(orderId);
        shipment = findByOrderId(orderId);

        while (shipment != null && shipment.getStatus() != targetStatus) {
            DeliveryStatus next = nextStatus(shipment.getStatus());
            if (next == null) {
                return;
            }

            trackingService.updateStatus(
                    shipment.getTrackingId(),
                    next,
                    shipment.getCourier() != null ? shipment.getCourier().getCurrentDistrict() : shipment.getShippingLocation(),
                    note != null && !note.isBlank() ? note : "Shipment synchronized from order status",
                    null);

            shipment = findByOrderId(orderId);
        }
    }

    private void ensureCourierAssigned(Long orderId) {
        Shipment shipment = findByOrderId(orderId);
        if (shipment != null && shipment.getCourier() == null) {
            assignCourier(shipment.getId());
        }
    }

    @Transactional
    public void autoAdvanceShipments() {
        for (Shipment shipment : shipmentRepository.findByStatusIn(java.util.List.of(
                DeliveryStatus.CREATED,
                DeliveryStatus.RIDER_ASSIGNED,
                DeliveryStatus.PICKED_UP,
                DeliveryStatus.IN_TRANSIT,
                DeliveryStatus.OUT_FOR_DELIVERY,
                DeliveryStatus.DELAYED
        ))) {
            DeliveryStatus nextStatus = nextStatus(shipment.getStatus());
            if (nextStatus == null) {
                continue;
            }
            if (nextStatus == DeliveryStatus.DELIVERED) {
                continue;
            }

            long minutesSinceUpdate = java.time.Duration.between(shipment.getUpdatedAt(), LocalDateTime.now()).toMinutes();
            long threshold = thresholdMinutes(nextStatus);
            if (minutesSinceUpdate >= threshold) {
                trackingService.updateStatus(
                        shipment.getTrackingId(),
                        nextStatus,
                        shipment.getCourier() != null ? shipment.getCourier().getCurrentDistrict() : "Transit Hub",
                        "Auto-updated by delivery scheduler",
                        null);
            }
        }
    }

    private DeliveryStatus nextStatus(DeliveryStatus current) {
        return switch (current) {
            case CREATED -> DeliveryStatus.RIDER_ASSIGNED;
            case RIDER_ASSIGNED -> DeliveryStatus.PICKED_UP;
            case PICKED_UP -> DeliveryStatus.IN_TRANSIT;
            case IN_TRANSIT -> DeliveryStatus.OUT_FOR_DELIVERY;
            case OUT_FOR_DELIVERY -> DeliveryStatus.DELIVERED;
            case DELAYED -> DeliveryStatus.IN_TRANSIT;
            case DELIVERED, FAILED_DELIVERY, RETURN_TO_SELLER, CANCELLED, CALL_NOT_PICKED, ADDRESS_NOT_FOUND -> null;
        };
    }

    private long thresholdMinutes(DeliveryStatus next) {
        return switch (next) {
            case CREATED, RIDER_ASSIGNED -> 1;
            case PICKED_UP -> 2;
            case IN_TRANSIT -> 3;
            case OUT_FOR_DELIVERY -> 4;
            case DELIVERED, FAILED_DELIVERY, RETURN_TO_SELLER, CANCELLED, CALL_NOT_PICKED, ADDRESS_NOT_FOUND -> 5;
            case DELAYED -> 6;
        };
    }

    private String generateTrackingId() {
        String trackingId;
        do {
            int suffix = java.util.concurrent.ThreadLocalRandom.current().nextInt(10000, 100000);
            trackingId = "NP-TRK-" + suffix;
        } while (shipmentRepository.existsByTrackingId(trackingId));
        return trackingId;
    }

    private LocalDateTime estimateDelivery(String shippingLocation) {
        if ("OUTSIDE".equalsIgnoreCase(shippingLocation)) {
            return LocalDateTime.now().plusDays(4);
        }
        return LocalDateTime.now().plusDays(2);
    }

    DeliveryResponseDTO toDeliveryResponse(Shipment shipment) {
        com.example.jhapcham.order.domain.Order order = shipment.getOrder();
        boolean isCod = order.getPaymentMethod() == com.example.jhapcham.order.domain.PaymentMethod.COD;
        
        String itemSummary = order.getItems().stream()
                .map(i -> i.getProductNameSnapshot() + " (x" + i.getQuantity() + ")")
                .collect(Collectors.joining(", "));

        return DeliveryResponseDTO.builder()
                .trackingId(shipment.getTrackingId())
                .deliveryStatus(shipment.getStatus())
                .customerName(order.getCustomerName())
                .customerPhone(order.getCustomerPhone())
                .destinationAddress(shipment.getDestinationAddress())
                .shippingLocation(shipment.getShippingLocation())
                .cashOnDelivery(isCod)
                .codAmount(isCod ? order.getGrandTotal() : java.math.BigDecimal.ZERO)
                .itemSummary(itemSummary)
                .courier(shipment.getCourier() != null ? courierService.toDto(shipment.getCourier(), null) : null)
                .notes(shipment.getNotes())
                .assignedAt(shipment.getAssignedAt())
                .deliveredAt(shipment.getDeliveredAt())
                .createdAt(shipment.getCreatedAt())
                .updatedAt(shipment.getUpdatedAt())
                .build();
    }
}
