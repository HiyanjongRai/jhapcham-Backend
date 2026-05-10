package com.example.jhapcham.delivery;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DeliveryRepository extends JpaRepository<Delivery, Long> {
    Optional<Delivery> findTopByShipment_IdOrderByCreatedAtDesc(Long shipmentId);
    List<Delivery> findByCourier_IdOrderByUpdatedAtDesc(Long courierId);
}
