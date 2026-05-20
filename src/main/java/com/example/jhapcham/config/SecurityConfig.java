package com.example.jhapcham.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

import lombok.RequiredArgsConstructor;
import com.example.jhapcham.delivery.CourierJwtAuthenticationFilter;
import com.example.jhapcham.delivery.CourierJwtService;
import com.example.jhapcham.delivery.CourierRepository;
import com.example.jhapcham.security.JwtProperties;
import com.example.jhapcham.security.JwtService;
import com.example.jhapcham.security.JwtAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final com.example.jhapcham.security.CustomUserDetailsService customUserDetailsService;
    private final CourierJwtService courierJwtService;
    private final CourierRepository courierRepository;

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

                // Public static assets only
                .requestMatchers(
                        "/product-images/**",
                        "/customer-profile/**",
                        "/review-images/**",
                        "/user-images/**",
                        "/seller_logos/**",
                        "/campaign-images/**",
                        "/banners/**",
                        "/dispute_evidence/**",
                        "/reports/**")
                .permitAll()

                // Public auth
                .requestMatchers(HttpMethod.POST,
                        "/api/auth/register/customer",
                        "/api/auth/register/seller",
                        "/api/auth/login",
                        "/api/auth/refresh",
                        "/api/auth/logout",
                        "/api/auth/google",
                        "/api/auth/forgot-password",
                        "/api/auth/verify-reset-otp",
                        "/api/auth/reset-password",
                        "/api/courier/login")
                .permitAll()

                .requestMatchers(HttpMethod.GET, "/api/tracking/**").permitAll()
                .requestMatchers(HttpMethod.GET,
                        "/api/payment/esewa/success",
                        "/api/payment/esewa/failure",
                        "/api/payment/khalti/order/success",
                        "/api/payment/khalti/commission/success").permitAll()

                // Public catalog
                .requestMatchers(HttpMethod.GET,
                        "/api/products",
                        "/api/products/page",
                        "/api/products/filter",
                        "/api/products/filter/page",
                        "/api/products/search",
                        "/api/products/search/page",
                        "/api/products/*",
                        "/api/products/slug/**",
                        "/api/products/*/views/count",
                        "/api/products/views/counts-by-product",
                        "/api/products/*/variants",
                        "/api/products/variants/*",
                        "/api/categories/**",
                        "/api/seller/*",
                        "/api/seller-profiles/**",
                        "/api/campaigns/**",
                        "/api/banners/**",
                        "/api/reviews/product/**")
                .permitAll()
                .requestMatchers(HttpMethod.POST, "/api/chat/**").permitAll()

                // Public order preview only. Order placement requires an authenticated customer.
                .requestMatchers(HttpMethod.POST, "/api/orders/preview").permitAll()

                // Public analytics
                .requestMatchers("/api/views/**", "/api/search-history/**").permitAll()

                // Authenticated APIs
                 .requestMatchers("/api/users/**").authenticated()
                .requestMatchers("/api/addresses/**").authenticated()
                .requestMatchers("/api/cart", "/api/cart/**").authenticated()
                .requestMatchers("/api/orders", "/api/orders/**").authenticated()
                .requestMatchers("/api/payment/**").authenticated()
                .requestMatchers("/api/wishlist", "/api/wishlist/**").authenticated()
                .requestMatchers("/api/follow/**").authenticated()
                .requestMatchers("/api/notifications", "/api/notifications/**").authenticated()
                .requestMatchers("/api/messages", "/api/messages/**").authenticated()
                .requestMatchers("/api/reports/**").authenticated()
                .requestMatchers("/api/disputes", "/api/disputes/**").authenticated()
                .requestMatchers("/api/promos/**").authenticated()
                .requestMatchers("/api/customers/**").authenticated()
                .requestMatchers("/api/sellers/**").authenticated()
                .requestMatchers("/api/inventory-alerts/**").authenticated()
                .requestMatchers("/api/sms/**").authenticated()
                .requestMatchers("/api/refunds", "/api/refunds/**").authenticated()
                .requestMatchers("/api/loyalty/**").authenticated()
                .requestMatchers("/api/seller/**").authenticated()
                .requestMatchers("/api/admin/**").authenticated()
                .requestMatchers("/api/delivery/**").authenticated()
                .requestMatchers("/api/shipment/**").authenticated()
                .requestMatchers("/api/courier/all", "/api/courier/assigned").authenticated()
                .requestMatchers("/api/tracking/update", "/api/tracking/update-status").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/products/**").authenticated()
                .requestMatchers(HttpMethod.PUT, "/api/products/**").authenticated()
                .requestMatchers(HttpMethod.PATCH, "/api/products/**").authenticated()
                .requestMatchers(HttpMethod.DELETE, "/api/products/**").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/reviews/**", "/api/reviews").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/reviews/user/**").authenticated()

                .anyRequest().authenticated());

        http.exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint((request, response, authException) -> {
                    response.sendError(jakarta.servlet.http.HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
                })
        );

        http.httpBasic(httpBasic -> httpBasic.disable());
        http.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        http.addFilterBefore(
                new CourierJwtAuthenticationFilter(courierJwtService, courierRepository),
                org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class);
        http.addFilterBefore(
                new JwtAuthenticationFilter(jwtService, customUserDetailsService),
                org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration config = new CorsConfiguration();
        List<String> allowedOrigins = jwtProperties.getAllowedOrigins().isEmpty()
                ? List.of(
                    "http://localhost:3000",
                    "http://localhost:3001",
                    "http://localhost:3002",
                    "http://127.0.0.1:5500")
                : jwtProperties.getAllowedOrigins();
        config.setAllowedOrigins(allowedOrigins);

        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Set-Cookie"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return source;
    }
}
