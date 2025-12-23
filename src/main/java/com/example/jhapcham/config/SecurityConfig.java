package com.example.jhapcham.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

import lombok.RequiredArgsConstructor;
import com.example.jhapcham.security.SimpleTokenService;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final SimpleTokenService simpleTokenService;
    private final com.example.jhapcham.security.CustomUserDetailsService customUserDetailsService;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http.csrf(csrf -> csrf.disable());
        http.cors(Customizer.withDefaults());

        http.authorizeHttpRequests(auth -> auth

                // OPTIONS always allowed
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                // STATIC FILES, FIXES 403
                .requestMatchers(
                        "/product-images/**",
                        "/uploads/product-images/**",
                        "/customer-profile/**",
                        "/uploads/customer-profile/**",
                        "/review-images/**",
                        "/uploads/review-images/**",
                        "/user-images/**",
                        "/api/orders/**",
                        "/uploads/user-images/**")
                .permitAll()

                // PUBLIC AUTH & LOGIN
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/customer/**").permitAll()
                .requestMatchers("/api/sellers/**").permitAll()
                .requestMatchers("/api/admin/**").permitAll()

                // PUBLIC PRODUCT APIs
                .requestMatchers("/api/products/**").permitAll()
                .requestMatchers("/api/categories/**").permitAll()

                // PUBLIC REVIEWS
                .requestMatchers("/api/reviews/**").permitAll()

                // CART
                .requestMatchers("/api/cart/**").permitAll()

                // ORDERS
                .requestMatchers("/orders/**").permitAll()

                // WISHLIST, NOTIFICATIONS
                .requestMatchers("/wishlist/**").permitAll()
                .requestMatchers("/notifications/**").permitAll()

                // USER PROFILE PUBLIC ROUTES
                .requestMatchers("/users/**").permitAll()

                // ANALYTICS
                .requestMatchers("/api/views/**").permitAll()
                .requestMatchers("/api/search-history/**").permitAll()
                .requestMatchers("/api/user-activity/**").permitAll()

                // ONLY THIS API REQUIRES LOGIN
                .requestMatchers("/api/users/me/**").authenticated()

                .requestMatchers("/api/seller-reviews/**").permitAll() // allow access to seller review endpoints

                .requestMatchers("/api/messages/**").authenticated()
                .requestMatchers("/api/auth/**").permitAll() // Al

                // EVERYTHING ELSE ALLOWED
                .anyRequest().permitAll());

        http.httpBasic(httpBasic -> httpBasic.disable());

        http.addFilterBefore(
                new com.example.jhapcham.security.SimpleTokenFilter(simpleTokenService, customUserDetailsService),
                org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(
                "http://localhost:3000",
                "http://localhost:3001",
                "http://localhost:3002",
                "http://127.0.0.1:5500"));

        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return source;
    }
}
