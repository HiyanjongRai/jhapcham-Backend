package com.example.jhapcham.admin.repository.controller;

import com.example.jhapcham.admin.repository.service.AdminService;
import com.example.jhapcham.user.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;


    @PostMapping("/register")
    public ResponseEntity<?> createAdmin(@RequestBody User admin) {
        User createdAdmin = adminService.createAdmin(admin);
        return ResponseEntity.ok(Map.of(
                "message", "Admin created successfully",
                "adminId", createdAdmin.getId()
        ));
    }

    // Admin login
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

    // Get all users
    @GetMapping("/users")
    public ResponseEntity<List<User>> getAllUsers() {
        List<User> users = adminService.getAllUsers();
        return ResponseEntity.ok(users);
    }
}
