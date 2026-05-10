package com.example.jhapcham.delivery;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ShipmentRepository extends JpaRepository<Shipment, Long> {
    Optional<Shipment> findByTrackingId(String trackingId);
    Optional<Shipment> findByOrder_Id(Long orderId);
    List<Shipment> findByCourier_IdOrderByUpdatedAtDesc(Long courierId);
    List<Shipment> findByStatusIn(Collection<DeliveryStatus> statuses);
    boolean existsByTrackingId(String trackingId);
}
