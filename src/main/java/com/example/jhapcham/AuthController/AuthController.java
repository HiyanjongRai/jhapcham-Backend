package com.example.jhapcham.AuthController;

import com.example.jhapcham.dto.AuthResponseDTO;
import com.example.jhapcham.dto.LoginRequestDTO;
import com.example.jhapcham.dto.RegisterRequestDTO;
import com.example.jhapcham.service.AuthService;
import com.example.jhapcham.user.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService auth;


    @PostMapping("/register/customer")
    public ResponseEntity<AuthResponseDTO> registerCustomer(@RequestBody RegisterRequestDTO req) {
        User u = auth.registerCustomer(req);
        return ResponseEntity.ok(new AuthResponseDTO(u.getId(), u.getUsername(), u.getEmail(), u.getRole(), u.getStatus(),
                "Customer created successfully"));
    }

    @PostMapping("/register/seller")
    public ResponseEntity<AuthResponseDTO> registerSeller(@RequestBody RegisterRequestDTO req) {
        User u = auth.registerSeller(req);
        return ResponseEntity.ok(new AuthResponseDTO(u.getId(), u.getUsername(), u.getEmail(), u.getRole(), u.getStatus(),
                "Seller created successfully. Waiting for admin approval."));
    }

    // Protect this route in SecurityConfig (ADMIN only) when you add Spring Security
    @PostMapping("/register/admin")
    public ResponseEntity<AuthResponseDTO> registerAdmin(@RequestBody RegisterRequestDTO req) {
        User u = auth.registerAdmin(req);
        return ResponseEntity.ok(new AuthResponseDTO(u.getId(), u.getUsername(), u.getEmail(), u.getRole(), u.getStatus(),
                "Admin created successfully"));
    }

    /* ---------------------- Login (single) ---------------------- */

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequestDTO req) {
        try {
            User u = auth.login(req);
            return ResponseEntity.ok(new AuthResponseDTO(u.getId(), u.getUsername(), u.getEmail(), u.getRole(), u.getStatus(),
                    "Login successful"));
        } catch (IllegalStateException e) { // pending/blocked seller
            return ResponseEntity.status(403).body(Map.of("message", e.getMessage()));
        } catch (RuntimeException e) {      // not found / bad password / inactive
            return ResponseEntity.status(401).body(Map.of("message", e.getMessage()));
        }
    }
}