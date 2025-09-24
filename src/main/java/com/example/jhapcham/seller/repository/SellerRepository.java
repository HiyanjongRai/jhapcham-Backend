package com.example.jhapcham.seller.repository;

import com.example.jhapcham.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SellerRepository extends JpaRepository<User, Long> {
    List<User> findByStatus(String status);
}
