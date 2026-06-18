package com.example.jhapcham.report.persistence;

import com.example.jhapcham.report.domain.SellerTrustScore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SellerTrustScoreRepository extends JpaRepository<SellerTrustScore, Long> {
}
