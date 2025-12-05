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

    /** Create new admin */
    public User createAdmin(User admin) {
        admin.setRole(Role.ADMIN);
        admin.setStatus(Status.ACTIVE);
        admin.setPassword(passwordEncoder.encode(admin.getPassword()));
        return adminRepository.save(admin);
    }

    /** Admin login */
    public User login(String usernameOrEmail, String password) {
        User admin = adminRepository
                .findByUsernameOrEmail(usernameOrEmail, usernameOrEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(password, admin.getPassword())) {
            throw new RuntimeException("Invalid password");
        }

        if (admin.getRole() != Role.ADMIN) {
            throw new RuntimeException("Unauthorized: Not an admin");
        }

        return admin;
    }

    /** Get all users (safe) */
    public List<User> getAllUsers() {
        return adminRepository.findAll();
    }

    /** Block a user */
    public User blockUser(Long userId) {
        User user = adminRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setStatus(Status.BLOCKED);
        return adminRepository.save(user);
    }

    /** Unblock a user */
    public User unblockUser(Long userId) {
        User user = adminRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setStatus(Status.ACTIVE);
        return adminRepository.save(user);
    }
}
