package com.example.jhapcham.banner.persistence;


import com.example.jhapcham.banner.application.*;
import com.example.jhapcham.banner.domain.*;
import com.example.jhapcham.banner.dto.*;
import com.example.jhapcham.banner.persistence.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface BannerRepository extends JpaRepository<Banner, Long> {

    @Query("""
            SELECT b FROM Banner b
            WHERE b.active = true
              AND (b.startDate IS NULL OR b.startDate <= :now)
              AND (b.endDate IS NULL OR b.endDate >= :now)
            ORDER BY b.priority ASC, b.id DESC
            """)
    List<Banner> findActiveBanners(LocalDateTime now);
}
