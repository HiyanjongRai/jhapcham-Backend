package com.example.jhapcham.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

        @Override
        public void addCorsMappings(@NonNull CorsRegistry registry) {
                registry.addMapping("/**")
                                .allowedOrigins(
                                                "http://localhost:3000",
                                                "http://127.0.0.1:5500")
                                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                                .allowedHeaders("*");
        }

        @Override
        public void addResourceHandlers(@NonNull ResourceHandlerRegistry registry) {

                String baseDir = "file:H:/Project/Ecomm/Jhapcham1/jhapcham-Backend/uploads/";

                // single generic handler for everything stored by FileStorageService
                registry.addResourceHandler("/uploads/**")
                                .addResourceLocations(baseDir);

                // Mapping "product-images" request path to the actual "products" folder on disk
                registry.addResourceHandler("/product-images/**")
                                .addResourceLocations(baseDir + "product-images/");

                registry.addResourceHandler("/products/**")
                                .addResourceLocations(baseDir + "products/");

                registry.addResourceHandler("/customer-profile/**")
                                .addResourceLocations(baseDir + "customer-profile/");

                registry.addResourceHandler("/review-images/**")
                                .addResourceLocations(baseDir + "review-images/");

                registry.addResourceHandler("/seller_logos/**")
                                .addResourceLocations(baseDir + "seller_logos/");

                registry.addResourceHandler("/campaign-images/**")
                                .addResourceLocations(baseDir + "campaign-images/");
        }

}