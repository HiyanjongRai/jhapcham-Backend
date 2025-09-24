package com.example.jhapcham.admin.repository.service;

import com.example.jhapcham.admin.repository.AdminRepository;
import com.example.jhapcham.user.model.Role;
import com.example.jhapcham.user.model.Status;
import com.example.jhapcham.user.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;

    public User createAdmin(User admin) {
        admin.setRole(Role.ADMIN);
        admin.setStatus(Status.ACTIVE);
        admin.setPassword(passwordEncoder.encode(admin.getPassword()));
        return adminRepository.save(admin);
    }

    public List<User> getAllUsers() {
        return adminRepository.findAll();
    }


    public User login(String usernameOrEmail, String password) {
        User admin = adminRepository
                .findByUsernameOrEmail(usernameOrEmail, usernameOrEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));


        if (!passwordEncoder.matches(password, admin.getPassword())) {
            throw new RuntimeException("Invalid password");
        }

        return admin;
    }
}
