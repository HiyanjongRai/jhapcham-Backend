package com.example.jhapcham.admin.repository.controller;

import com.example.jhapcham.admin.repository.service.AdminService;
import com.example.jhapcham.user.model.Status;
import com.example.jhapcham.user.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    // ------------------ Admin registration ------------------
    @PostMapping("/register")
    public ResponseEntity<?> createAdmin(@RequestBody User admin) {
        User createdAdmin = adminService.createAdmin(admin);
        return ResponseEntity.ok(Map.of(
                "message", "Admin created successfully",
                "adminId", createdAdmin.getId()
        ));
    }

    // ------------------ Admin login ------------------
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> loginData) {
        String usernameOrEmail = loginData.get("usernameOrEmail");
        String password = loginData.get("password");

        User admin = adminService.login(usernameOrEmail, password);
        return ResponseEntity.ok(Map.of(
                "message", "Login successful",
                "adminId", admin.getId(),
                "username", admin.getUsername(),
                "email", admin.getEmail(),
                "role", admin.getRole().name()
        ));
    }

    // ------------------ Get all users (safe) ------------------
    @GetMapping("/users")
    public ResponseEntity<List<AdminUserDTO>> getAllUsers() {
        List<AdminUserDTO> users = adminService.getAllUsers()
                .stream()
                .map(u -> AdminUserDTO.builder()
                        .userId(u.getId())
                        .username(u.getUsername())
                        .fullName(u.getFullName())
                        .email(u.getEmail())
                        .contactNumber(u.getContactNumber())
                        .role(u.getRole())
                        .status(u.getStatus())
                        .build())
                .sorted(Comparator.comparing(AdminUserDTO::getFullName, Comparator.nullsLast(String::compareToIgnoreCase)))
                .collect(Collectors.toList());

        return ResponseEntity.ok(users);
    }

    // ------------------ Block user ------------------
    @PostMapping("/users/{userId}/block")
    public ResponseEntity<?> blockUser(@PathVariable Long userId) {
        User user = adminService.blockUser(userId);
        return ResponseEntity.ok(Map.of(
                "message", "User blocked successfully",
                "userId", user.getId(),
                "status", user.getStatus().name()
        ));
    }

    // ------------------ Unblock user ------------------
    @PostMapping("/users/{userId}/unblock")
    public ResponseEntity<?> unblockUser(@PathVariable Long userId) {
        User user = adminService.unblockUser(userId);
        return ResponseEntity.ok(Map.of(
                "message", "User unblocked successfully",
                "userId", user.getId(),
                "status", user.getStatus().name()
        ));
    }
}
