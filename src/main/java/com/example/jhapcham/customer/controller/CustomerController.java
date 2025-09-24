package com.example.jhapcham.customer.controller;

import com.example.jhapcham.user.model.User;
import com.example.jhapcham.user.model.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/customer")
@CrossOrigin(origins = {"http://127.0.0.1:5500", "http://localhost:5500"})
public class CustomerController {

    @Autowired
    private UserService userService;

    // Register Customer
    @PostMapping("/register")
    public ResponseEntity<?> registerCustomer(@RequestBody User user) {
        try {
            userService.registerCustomer(user);
            return ResponseEntity.ok("Customer created successfully!");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // Customer Login
    @PostMapping("/login")
    public ResponseEntity<?> loginCustomer(@RequestBody Map<String, String> loginData) {
        String usernameOrEmail = loginData.get("usernameOrEmail");
        String password = loginData.get("password");

        try {
            String message = userService.loginCustomer(usernameOrEmail, password);
            return ResponseEntity.ok(Map.of("message", message));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

}
