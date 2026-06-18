package com.example.jhapcham.banner.persistence;


import com.example.jhapcham.banner.application.*;
import com.example.jhapcham.banner.domain.*;
import com.example.jhapcham.banner.dto.*;
import com.example.jhapcham.banner.persistence.*;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BannerProductRepository extends JpaRepository<BannerProduct, Long> {
    List<BannerProduct> findByBannerIdOrderByDisplayOrderAsc(Long bannerId);

    Optional<BannerProduct> findByBannerIdAndProductId(Long bannerId, Long productId);

    boolean existsByBannerIdAndProductId(Long bannerId, Long productId);
}
