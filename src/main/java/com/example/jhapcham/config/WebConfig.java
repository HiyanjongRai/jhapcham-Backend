package com.example.jhapcham.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(
                        "http://localhost:3000",
                        "http://127.0.0.1:5500"
                )
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {

        // FIXED PRODUCT IMAGES PATH
        registry.addResourceHandler("/product-images/**")
                .addResourceLocations("file:H:/Project/Ecomm/jhapcham/uploads/products/");

        registry.addResourceHandler("/uploads/product-images/**")
                .addResourceLocations("file:H:/Project/Ecomm/jhapcham/uploads/products/");

        // CUSTOMER PROFILE IMAGES
        registry.addResourceHandler("/customer-profile/**")
                .addResourceLocations("file:H:/Project/Ecomm/jhapcham/uploads/customer-profile/");

        registry.addResourceHandler("/uploads/customer-profile/**")
                .addResourceLocations("file:H:/Project/Ecomm/jhapcham/uploads/customer-profile/");

        // REVIEW IMAGES
        registry.addResourceHandler("/review-images/**")
                .addResourceLocations("file:H:/Project/Ecomm/jhapcham/uploads/review-images/");

        registry.addResourceHandler("/uploads/review-images/**")
                .addResourceLocations("file:H:/Project/Ecomm/jhapcham/uploads/review-images/");

        // Serve seller logos
        registry.addResourceHandler("/seller-logos/**")
                .addResourceLocations("file:H:/Project/Ecomm/jhapcham/uploads/seller_logos/");
    }
}
