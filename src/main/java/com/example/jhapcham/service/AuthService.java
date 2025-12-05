package com.example.jhapcham.service;

import com.example.jhapcham.dto.LoginRequestDTO;
import com.example.jhapcham.dto.RegisterRequestDTO;
import com.example.jhapcham.user.model.Role;
import com.example.jhapcham.user.model.Status;
import com.example.jhapcham.user.model.User;
import com.example.jhapcham.user.model.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;

    /* ---------- Register ---------- */

    public User registerCustomer(RegisterRequestDTO req) {

        if (users.existsByUsername(req.username()))
            throw new RuntimeException("Username already exists");

        if (users.existsByEmail(req.email()))
            throw new RuntimeException("Email already exists");

        User u = new User();
        u.setUsername(req.username());
        u.setFullName(req.fullName());
        u.setEmail(req.email());
        u.setRole(Role.CUSTOMER);
        u.setStatus(Status.ACTIVE);
        u.setPassword(passwordEncoder.encode(req.password()));

        return users.save(u);
    }


    public User registerSeller(RegisterRequestDTO req) {
        validateUniqueness(req);
        User u = baseUserFrom(req);
        u.setRole(Role.SELLER);
        u.setStatus(Status.PENDING);        // seller needs approval
        u.setPassword(passwordEncoder.encode(req.password()));
        return users.save(u);
    }

    /** Only allow from a secured admin-only flow or during bootstrap */
    public User registerAdmin(RegisterRequestDTO req) {
        validateUniqueness(req);
        User u = baseUserFrom(req);
        u.setRole(Role.ADMIN);
        u.setStatus(Status.ACTIVE);
        u.setPassword(passwordEncoder.encode(req.password()));
        return users.save(u);
    }

    private void validateUniqueness(RegisterRequestDTO req) {
        if (users.existsByUsername(req.username())) throw new RuntimeException("Username already exists");
        if (users.existsByEmail(req.email())) throw new RuntimeException("Email already exists");
    }

    private User baseUserFrom(RegisterRequestDTO req) {
        User u = new User();
        u.setUsername(req.username());
        u.setEmail(req.email());
        return u;
    }

    /* ---------- Login (single logic) ---------- */

    public User login(LoginRequestDTO req) {
        User user = users.findByUsernameOrEmail(req.usernameOrEmail(), req.usernameOrEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(req.password(), user.getPassword()))
            throw new RuntimeException("Invalid password");

        // Status/role policy
        if (user.getRole() == Role.SELLER) {
            if (user.getStatus() == Status.PENDING)
                throw new IllegalStateException("Your application is pending approval.");
            if (user.getStatus() == Status.BLOCKED)
                throw new IllegalStateException("Your account is blocked. Contact support.");
        } else { // ADMIN / CUSTOMER
            if (user.getStatus() != Status.ACTIVE)
                throw new RuntimeException("Account is not active");
        }

        return user;
    }
}