package com.example.jhapcham.seller.Service;

import com.example.jhapcham.user.model.Role;
import com.example.jhapcham.user.model.Status;
import com.example.jhapcham.user.model.User;
import com.example.jhapcham.user.model.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SellerService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;


    public User createSeller(User seller) {
        if (userRepository.existsByUsername(seller.getUsername())) {
            throw new RuntimeException("Username already exists!");
        }
        if (userRepository.existsByEmail(seller.getEmail())) {
            throw new RuntimeException("Email already exists!");
        }
        seller.setPassword(passwordEncoder.encode(seller.getPassword()));
        seller.setRole(Role.SELLER);
        seller.setStatus(Status.PENDING);
        return userRepository.save(seller);
    }
    public User authenticateSeller(String usernameOrEmail, String rawPassword) {
        User user = userRepository.findByUsernameOrEmail(usernameOrEmail, usernameOrEmail)
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));

        if (user.getRole() != Role.SELLER) {
            throw new RuntimeException("Invalid credentials");
        }

        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }

        if (user.getStatus() == Status.PENDING) {
            throw new IllegalStateException("Your application is pending approval. Please wait.");
        }
        if (user.getStatus() == Status.BLOCKED) {
            throw new IllegalStateException("Your account is blocked. Contact support.");
        }

        // Only ACTIVE sellers reach here
        return user;
    }
}
