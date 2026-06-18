package com.example.jhapcham.seller.persistence;


import com.example.jhapcham.seller.application.*;
import com.example.jhapcham.seller.domain.*;
import com.example.jhapcham.seller.dto.*;
import com.example.jhapcham.seller.persistence.*;
import com.example.jhapcham.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SellerApplicationRepository extends JpaRepository<SellerApplication, Long> {

    boolean existsByUser(User user);

    List<SellerApplication> findByStatus(ApplicationStatus status);

    Optional<SellerApplication> findByUser(User user);

    Optional<SellerApplication> findByUserId(Long userId);


}