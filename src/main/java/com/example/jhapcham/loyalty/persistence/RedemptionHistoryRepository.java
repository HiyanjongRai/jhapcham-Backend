package com.example.jhapcham.loyalty.persistence;


import com.example.jhapcham.loyalty.application.*;
import com.example.jhapcham.loyalty.domain.*;
import com.example.jhapcham.loyalty.dto.*;
import com.example.jhapcham.loyalty.persistence.*;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RedemptionHistoryRepository extends JpaRepository<RedemptionHistory, Long> {
    List<RedemptionHistory> findByOrderIdAndRestoredFalse(Long orderId);
}
