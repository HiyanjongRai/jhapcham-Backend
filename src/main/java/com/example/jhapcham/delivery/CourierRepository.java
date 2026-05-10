package com.example.jhapcham.delivery;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CourierRepository extends JpaRepository<Courier, Long> {
    Optional<Courier> findByEmailIgnoreCase(String email);
    List<Courier> findByActiveTrue();
}
