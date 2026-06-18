package com.example.jhapcham.delivery.persistence;


import com.example.jhapcham.delivery.application.*;
import com.example.jhapcham.delivery.domain.*;
import com.example.jhapcham.delivery.dto.*;
import com.example.jhapcham.delivery.persistence.*;
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
