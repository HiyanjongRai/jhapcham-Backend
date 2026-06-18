package com.example.jhapcham.report.persistence;

import com.example.jhapcham.report.domain.SellerPenalty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SellerPenaltyRepository extends JpaRepository<SellerPenalty, Long> {
    List<SellerPenalty> findBySellerId(Long sellerId);
}
