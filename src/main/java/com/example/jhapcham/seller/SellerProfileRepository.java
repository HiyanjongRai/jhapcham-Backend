package com.example.jhapcham.seller;

import com.example.jhapcham.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SellerProfileRepository extends JpaRepository<SellerProfile, Long> {

    Optional<SellerProfile> findByUser(User user);

    Optional<SellerProfile> findByUserId(Long userId);

}
