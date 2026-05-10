package com.example.jhapcham.delivery;

import com.example.jhapcham.Error.AuthorizationException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class CurrentCourierService {

    public Courier requireCourier(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new AuthorizationException("Courier authentication is required");
        }
        if (authentication.getPrincipal() instanceof Courier courier) {
            return courier;
        }
        throw new AuthorizationException("Courier authentication is required");
    }
}
