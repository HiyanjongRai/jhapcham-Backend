package com.example.jhapcham.seller.repository;

import com.example.jhapcham.seller.model.ApplicationStatus;
import com.example.jhapcham.seller.model.SellerApplication;
import com.example.jhapcham.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SellerApplicationRepository extends JpaRepository<SellerApplication, Long> {
    List<SellerApplication> findByStatus(ApplicationStatus status);
    Optional<SellerApplication> findByUser(User user);
    boolean existsByUser(User user);
    Optional<SellerApplication> findByUserId(Long userId);

}
