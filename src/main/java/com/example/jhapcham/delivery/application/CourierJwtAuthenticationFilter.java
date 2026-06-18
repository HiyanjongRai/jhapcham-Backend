package com.example.jhapcham.delivery.application;


import com.example.jhapcham.delivery.application.*;
import com.example.jhapcham.delivery.domain.*;
import com.example.jhapcham.delivery.dto.*;
import com.example.jhapcham.delivery.persistence.*;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@RequiredArgsConstructor
@Slf4j
public class CourierJwtAuthenticationFilter extends OncePerRequestFilter {

    private final CourierJwtService courierJwtService;
    private final CourierRepository courierRepository;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            String token = authHeader.substring(7);
            try {
                if (courierJwtService.isCourierToken(token)) {
                    String email = courierJwtService.extractSubject(token);
                    courierRepository.findByEmailIgnoreCase(email).ifPresent(courier -> {
                        if (courier.isActive()) {
                            var authentication = new UsernamePasswordAuthenticationToken(
                                    courier,
                                    null,
                                    List.of(new SimpleGrantedAuthority("ROLE_COURIER")));
                            SecurityContextHolder.getContext().setAuthentication(authentication);
                        }
                    });
                }
            } catch (JwtException | IllegalArgumentException ex) {
                SecurityContextHolder.clearContext();
                log.debug("Rejected courier token: {}", ex.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }
}
