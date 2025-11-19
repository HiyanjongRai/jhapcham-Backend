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
import org.springframework.web.filter.CorsFilter;

import java.util.List;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http.csrf(csrf -> csrf.disable());
        http.cors(cors -> cors.configure(http));

        http.authorizeHttpRequests(auth -> auth

                // Public OPTIONS
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                // Auth APIs
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/auth/login").permitAll()
                .requestMatchers("/api/auth/login").permitAll()
                .requestMatchers("/api/customer/register", "/api/customer/login").permitAll()
                .requestMatchers("/api/sellers/register", "/api/sellers/login").permitAll()
                .requestMatchers("/api/admin/login", "/api/admin/register").permitAll()

                // Seller application submit
                .requestMatchers(HttpMethod.POST, "/api/sellers/application").permitAll()

                // Allow seller to check application status
                .requestMatchers("/api/seller/application/**").permitAll()

                // check  pending application
                .requestMatchers("/api/admin/sellers/**").permitAll()

                // Products and categories public
                .requestMatchers(HttpMethod.GET, "/api/products/**", "/api/categories/**").permitAll()

                // Product CRUD (public for now)
                .requestMatchers("/api/products/**").permitAll()

                // Cart public
                .requestMatchers("/cart/**").permitAll()
                .requestMatchers("/api/cart/**").permitAll()

                // Ratings and likes
                .requestMatchers("/api/products/*/ratings/**").permitAll()
                .requestMatchers("/likes/**").permitAll()

                // Orders public
                .requestMatchers("/orders/**").permitAll()

                .requestMatchers("/product-images/**").permitAll()


                // allow images
                .requestMatchers("/review-images/**").permitAll()

                .requestMatchers("/uploads/**").permitAll()
                .requestMatchers("/product-images/**").permitAll()
                .requestMatchers("/api/products/images/**").permitAll()

                // allow public APIs
                .requestMatchers("/api/views/**").permitAll()
                .requestMatchers("/api/reviews/**").permitAll()
                .requestMatchers("/api/products/**").permitAll()

                // allow cart add for testing
                .requestMatchers("/api/cart/**").permitAll()

                //  Review
                .requestMatchers(
                        "/api/reviews/**",
                        "/api/products/*/ratings/**",
                        "/api/products/*/reports/**",
                        "/orders/*/tracking",
                        "/orders/user/**"
                ).permitAll()

                // User activity logs
                .requestMatchers("/api/user-activity/**", "/userActivity/**", "/activity/**").permitAll()

                .requestMatchers("/product-images/**", "/product-images/*").permitAll()

                // Views and search history
                .requestMatchers("/api/views/**", "/api/search-history/**").permitAll()


                .requestMatchers(HttpMethod.PATCH, "/orders/**").permitAll()

                // Images and uploads
                .requestMatchers("/uploads/**", "/images/**").permitAll()

                // Everything else needs authentication
                .anyRequest().authenticated()
        );

        http.httpBasic(Customizer.withDefaults());

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.setAllowedOriginPatterns(List.of("http://localhost:*", "http://127.0.0.1:*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public CorsFilter corsFilter() {

        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(
                "http://localhost:3000",
                "http://localhost:3001",
                "http://localhost:3002",
                "http://localhost:3003",
                "http://127.0.0.1:5500"
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(false);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return new CorsFilter(source);
    }
}
