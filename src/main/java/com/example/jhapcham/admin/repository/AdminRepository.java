package com.example.jhapcham.admin.repository;

import com.example.jhapcham.user.model.Role;
import com.example.jhapcham.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;

public interface AdminRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsernameAndRole(String username, Role role);
    List<User> findByRole(Role role);
    Optional<User> findByUsernameOrEmail(String username, String email);


}
