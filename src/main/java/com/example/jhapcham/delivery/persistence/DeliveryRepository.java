package com.example.jhapcham.delivery.persistence;


import com.example.jhapcham.delivery.application.*;
import com.example.jhapcham.delivery.domain.*;
import com.example.jhapcham.delivery.dto.*;
import com.example.jhapcham.delivery.persistence.*;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DeliveryRepository extends JpaRepository<Delivery, Long> {
    Optional<Delivery> findTopByShipment_IdOrderByCreatedAtDesc(Long shipmentId);
    List<Delivery> findByCourier_IdOrderByUpdatedAtDesc(Long courierId);
}
