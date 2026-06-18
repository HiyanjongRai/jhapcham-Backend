package com.example.jhapcham.report.persistence;

import com.example.jhapcham.report.domain.CustomerFlag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CustomerFlagRepository extends JpaRepository<CustomerFlag, Long> {
    List<CustomerFlag> findByCustomerId(Long customerId);
}
