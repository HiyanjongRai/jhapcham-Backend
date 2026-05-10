package com.example.jhapcham.delivery;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TrackingRepository extends JpaRepository<TrackingHistory, Long> {
    List<TrackingHistory> findByShipment_TrackingIdOrderByCreatedAtAsc(String trackingId);
    List<TrackingHistory> findByShipment_IdOrderByCreatedAtAsc(Long shipmentId);
}
