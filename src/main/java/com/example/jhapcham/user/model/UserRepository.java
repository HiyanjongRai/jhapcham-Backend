package com.example.jhapcham.user.model;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    Optional<User> findByUsernameOrEmail(String username, String email);

    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    java.util.List<User> findByRole(Role role);

}