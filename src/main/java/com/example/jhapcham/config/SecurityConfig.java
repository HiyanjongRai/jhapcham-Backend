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
                .cors(cors -> cors.configure(http))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // Public auth
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/customer/register", "/api/sellers/register", "/api/admin/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/customer/login", "/api/sellers/login").permitAll()

                        // Public read-only catalog (adjust patterns to your app)
                        .requestMatchers(HttpMethod.GET, "/api/products/**", "/api/categories/**").permitAll()

                        // Cart endpoints public (if you want guest cart), else require login
                        .requestMatchers(HttpMethod.POST, "/cart/add", "/cart/remove").permitAll()
                        .requestMatchers(HttpMethod.GET,  "/cart").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll() // allow preflight
                        .requestMatchers(HttpMethod.POST, "/api/customer/register").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/customer/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/sellers/register").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/sellers/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/products/add").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/sellers/application").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/admin/register").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/admin/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/cart/add").permitAll()
                        .requestMatchers(HttpMethod.GET, "/cart").permitAll()
                        .requestMatchers(HttpMethod.POST, "/cart/remove").permitAll()

                        // Allow product creation with JSON or multipart form-data (image + details)
                        .requestMatchers(HttpMethod.POST, "/api/products/create/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/products/create-form/**").permitAll()
                        // Allow product image upload endpoints (optional)
                        .requestMatchers(HttpMethod.POST, "/api/products/*/*/image").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/products/upload-image").permitAll()
                        // Allow reading product data if you expose GETs
                        .requestMatchers("/api/products/update/**").permitAll()
                        .requestMatchers("/api/products/delete/**").permitAll()
                        .requestMatchers("/api/products/**").permitAll()

                        .requestMatchers(HttpMethod.GET, "/api/products/**").permitAll()
                        .requestMatchers("/api/admin/**").permitAll()

                        .requestMatchers("/api/cart/**").permitAll()


                        //  Permit Reviews API
                        .requestMatchers(HttpMethod.GET, "/api/products/*/ratings/**").permitAll()


                        // Permit User Activity API
                            .requestMatchers(HttpMethod.GET,  "/api/user-activity/**", "/userActivity/**").permitAll()
                            .requestMatchers(HttpMethod.POST, "/api/user-activity/**", "/userActivity/**").permitAll()
                            .requestMatchers(HttpMethod.PUT,  "/api/user-activity/**", "/userActivity/**").permitAll()
                            .requestMatchers(HttpMethod.DELETE, "/api/user-activity/**", "/userActivity/**").permitAll()
                            .requestMatchers(HttpMethod.GET, "/activity/**").permitAll()


                        .requestMatchers("/likes/**").permitAll()
                        .requestMatchers("/orders/**").permitAll()


                        .requestMatchers(HttpMethod.GET, "/productLike/product-likes").permitAll()

                            // Permit User Product Interaction API
                            .requestMatchers(HttpMethod.POST, "/interactions/**").permitAll()
                            .requestMatchers(HttpMethod.GET, "/interactions/**").permitAll()

                                // --- Allow all order endpoints (add this before .anyRequest().authenticated()) ---
                                .requestMatchers("/orders/**").permitAll()
                        .requestMatchers("/api/search-history/**").permitAll()


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
        config.setAllowedOriginPatterns(List.of("http://127.0.0.1:*", "http://localhost:*"));
        config.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));
        config.setAllowedHeaders(List.of("*"));


        config.setAllowedOriginPatterns(List.of("http://127.0.0.1:5500", "http://127.0.0.1:5501", "http://localhost:5500", "http://localhost:5501"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }




}
