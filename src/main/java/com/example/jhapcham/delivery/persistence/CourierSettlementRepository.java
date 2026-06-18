package com.example.jhapcham.delivery.persistence;


import com.example.jhapcham.delivery.application.*;
import com.example.jhapcham.delivery.domain.*;
import com.example.jhapcham.delivery.dto.*;
import com.example.jhapcham.delivery.persistence.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface CourierSettlementRepository extends JpaRepository<CourierSettlement, Long> {

    List<CourierSettlement> findByCourier_IdAndRemittedToHubFalse(Long courierId);

    List<CourierSettlement> findByCourier_IdOrderByCreatedAtDesc(Long courierId);

    Optional<CourierSettlement> findByTrackingId(String trackingId);

    @Query("SELECT COALESCE(SUM(s.collectedAmount), 0) FROM CourierSettlement s WHERE s.courier.id = :courierId AND s.remittedToHub = false")
    BigDecimal sumPendingCashByCourierId(@Param("courierId") Long courierId);
}
