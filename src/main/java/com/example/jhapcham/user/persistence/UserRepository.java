package com.example.jhapcham.user.persistence;


import com.example.jhapcham.user.application.*;
import com.example.jhapcham.user.domain.*;
import com.example.jhapcham.user.dto.*;
import com.example.jhapcham.user.persistence.*;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    Optional<User> findByUsernameOrEmail(String username, String email);

    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    java.util.List<User> findByRole(Role role);

    long countByRole(Role role);

}
