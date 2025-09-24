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

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll() // allow preflight
                        .requestMatchers(HttpMethod.POST, "/api/customer/register").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/customer/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/sellers/register").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/sellers/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/products/add").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/sellers/application").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/admin/register").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/admin/login").permitAll()
                        // Allow product creation with JSON or multipart form-data (image + details)
                        .requestMatchers(HttpMethod.POST, "/api/products/create/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/products/create-form/**").permitAll()
                        // Allow product image upload endpoints (optional)
                        .requestMatchers(HttpMethod.POST, "/api/products/*/*/image").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/products/upload-image").permitAll()
                        // Allow reading product data if you expose GETs
                        .requestMatchers(HttpMethod.GET, "/api/products/**").permitAll()
                        .requestMatchers("/api/admin/**").permitAll()
                        .anyRequest().authenticated()
                )
                .httpBasic(Customizer.withDefaults())
                .cors(Customizer.withDefaults());

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.setAllowedOriginPatterns(List.of("http://127.0.0.1:5500", "http://localhost:5500"));
        config.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));
        config.setAllowedHeaders(List.of("*"));


        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
