package com.example.jhapcham.delivery;

import com.example.jhapcham.Error.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeliveryService {

    private final DeliveryRepository deliveryRepository;
    private final ShipmentService shipmentService;
    private final CourierService courierService;

    @Transactional
    public DeliveryResponseDTO createDelivery(DeliveryRequestDTO dto) {
        Shipment shipment = shipmentService.getShipmentEntity(dto.getShipmentId());
        Courier courier = dto.getCourierId() != null
                ? courierService.getCourierOrFail(dto.getCourierId())
                : courierService.getRandomActiveCourier();

        Delivery delivery = Delivery.builder()
                .shipment(shipment)
                .courier(courier)
                .status(shipment.getStatus())
                .notes(dto.getNotes() != null ? dto.getNotes() : "Delivery task created")
                .attemptCount(0)
                .assignedAt(courier != null ? java.time.LocalDateTime.now() : null)
                .build();
        deliveryRepository.save(delivery);
        shipment.getDeliveries().add(delivery);

        if (courier != null) {
            shipment.setCourier(courier);
            shipment.setAssignedAt(java.time.LocalDateTime.now());
        }

        return shipmentService.toDeliveryResponse(shipment);
    }

    @Transactional(readOnly = true)
    public DeliveryResponseDTO getDelivery(Long deliveryId) {
        Delivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery not found"));
        return shipmentService.toDeliveryResponse(delivery.getShipment());
    }
}
