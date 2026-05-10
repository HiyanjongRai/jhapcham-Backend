package com.example.jhapcham.delivery;

import com.example.jhapcham.Error.BusinessValidationException;
import com.example.jhapcham.Error.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CourierService {

    private final CourierRepository courierRepository;
    private final ShipmentRepository shipmentRepository;
    private final PasswordEncoder passwordEncoder;
    private final CourierJwtService courierJwtService;

    @Transactional
    public CourierDTO createCourier(CourierDTO dto) {
        if (dto.getEmail() == null || dto.getEmail().isBlank()) {
            throw new BusinessValidationException("Courier email is required");
        }
        if (dto.getPassword() == null || dto.getPassword().length() < 6) {
            throw new BusinessValidationException("Courier password must be at least 6 characters");
        }
        if (courierRepository.findByEmailIgnoreCase(dto.getEmail()).isPresent()) {
            throw new BusinessValidationException("Courier email already exists");
        }

        Courier courier = Courier.builder()
                .fullName(dto.getFullName())
                .email(dto.getEmail().trim().toLowerCase())
                .phoneNumber(dto.getPhoneNumber())
                .currentDistrict(dto.getCurrentDistrict())
                .vehicleType(dto.getVehicleType())
                .active(dto.getActive() == null || dto.getActive())
                .passwordHash(passwordEncoder.encode(dto.getPassword()))
                .build();

        return toDto(courierRepository.save(courier), null);
    }

    @Transactional
    public CourierDTO login(CourierDTO dto) {
        Courier courier = courierRepository.findByEmailIgnoreCase(dto.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Courier account not found"));

        if (!courier.isActive()) {
            throw new BusinessValidationException("Courier account is inactive");
        }
        if (!passwordEncoder.matches(dto.getPassword(), courier.getPasswordHash())) {
            throw new BusinessValidationException("Invalid courier credentials");
        }

        courier.setLastLoginAt(LocalDateTime.now());
        courierRepository.save(courier);

        return toDto(courier, courierJwtService.generateCourierToken(courier));
    }

    @Transactional(readOnly = true)
    public List<CourierDTO> getAllCouriers() {
        return courierRepository.findAll().stream()
                .map(courier -> toDto(courier, null))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<DeliveryResponseDTO> getAssignedShipments(Long courierId) {
        return shipmentRepository.findByCourier_IdOrderByUpdatedAtDesc(courierId).stream()
                .map(this::toDeliveryResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Courier getCourierOrFail(Long courierId) {
        return courierRepository.findById(courierId)
                .orElseThrow(() -> new ResourceNotFoundException("Courier not found"));
    }

    @Transactional(readOnly = true)
    public Courier getRandomActiveCourier() {
        List<Courier> activeCouriers = courierRepository.findByActiveTrue();
        if (activeCouriers.isEmpty()) {
            return null;
        }
        int index = java.util.concurrent.ThreadLocalRandom.current().nextInt(activeCouriers.size());
        return activeCouriers.get(index);
    }

    CourierDTO toDto(Courier courier, String token) {
        return CourierDTO.builder()
                .id(courier.getId())
                .fullName(courier.getFullName())
                .email(courier.getEmail())
                .phoneNumber(courier.getPhoneNumber())
                .currentDistrict(courier.getCurrentDistrict())
                .vehicleType(courier.getVehicleType())
                .active(courier.isActive())
                .assignedShipmentCount(courier.getAssignedShipments() != null ? courier.getAssignedShipments().size() : 0)
                .token(token)
                .build();
    }

    private DeliveryResponseDTO toDeliveryResponse(Shipment shipment) {
        com.example.jhapcham.order.Order order = shipment.getOrder();
        boolean isCod = order.getPaymentMethod() == com.example.jhapcham.order.PaymentMethod.COD;
        
        String itemSummary = order.getItems().stream()
                .map(i -> i.getProductNameSnapshot() + " (x" + i.getQuantity() + ")")
                .collect(java.util.stream.Collectors.joining(", "));

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
                .courier(shipment.getCourier() != null ? toDto(shipment.getCourier(), null) : null)
                .notes(shipment.getNotes())
                .assignedAt(shipment.getAssignedAt())
                .deliveredAt(shipment.getDeliveredAt())
                .createdAt(shipment.getCreatedAt())
                .updatedAt(shipment.getUpdatedAt())
                .build();
    }
}
