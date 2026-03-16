package com.example.jhapcham.user.model;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface UserAddressRepository extends JpaRepository<UserAddress, Long> {
    List<UserAddress> findByUserIdOrderByIsDefaultDesc(Long userId);
}
