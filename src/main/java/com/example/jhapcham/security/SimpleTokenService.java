package com.example.jhapcham.security;

import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.UUID;

@Service
public class SimpleTokenService {

    // Token -> Username
    private final Map<String, String> tokenStore = new ConcurrentHashMap<>();

    // Token -> Role (e.g. "CUSTOMER")
    private final Map<String, String> roleStore = new ConcurrentHashMap<>();

    public String generateToken(String username, String role) {
        String token = UUID.randomUUID().toString();
        tokenStore.put(token, username);
        // Ensure we store it safely
        roleStore.put(token, role != null ? role : "USER");
        return token;
    }

    public String getUsername(String token) {
        return tokenStore.get(token);
    }

    public String getRole(String token) {
        return roleStore.get(token);
    }

    // Optional: Logout method to remove token
    public void invalidateToken(String token) {
        tokenStore.remove(token);
        roleStore.remove(token);
    }
}
