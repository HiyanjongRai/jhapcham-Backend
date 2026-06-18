package com.example.jhapcham.delivery.persistence;


import com.example.jhapcham.delivery.application.*;
import com.example.jhapcham.delivery.domain.*;
import com.example.jhapcham.delivery.dto.*;
import com.example.jhapcham.delivery.persistence.*;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TrackingRepository extends JpaRepository<TrackingHistory, Long> {
    List<TrackingHistory> findByShipment_TrackingIdOrderByCreatedAtAsc(String trackingId);
    List<TrackingHistory> findByShipment_IdOrderByCreatedAtAsc(Long shipmentId);
}
