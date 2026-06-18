package com.example.jhapcham.user.persistence;


import com.example.jhapcham.user.application.*;
import com.example.jhapcham.user.domain.*;
import com.example.jhapcham.user.dto.*;
import com.example.jhapcham.user.persistence.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface UserAddressRepository extends JpaRepository<UserAddress, Long> {
    List<UserAddress> findByUserIdOrderByIsDefaultDesc(Long userId);
}
