package com.example.jhapcham.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

@Configuration
public class WebConfig implements WebMvcConfigurer {

        @Value("${file.upload.dir:H:/Project/Ecomm/Jhapcham Backend/jhapcham-Backend/uploads}")
        private String uploadDir;

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

                String configuredBaseDir = Path.of(uploadDir).toAbsolutePath().normalize().toUri().toString();
                String projectBaseDir = Path.of("H:/Project/Ecomm/Jhapcham Backend/jhapcham-Backend/uploads")
                                .toAbsolutePath()
                                .normalize()
                                .toUri()
                                .toString();

                // Mapping "product-images" request path to the actual "products" folder on disk
                registry.addResourceHandler("/product-images/**")
                                .addResourceLocations(configuredBaseDir + "product-images/", projectBaseDir + "product-images/");

                registry.addResourceHandler("/products/**")
                                .addResourceLocations(configuredBaseDir + "products/", projectBaseDir + "products/");

                registry.addResourceHandler("/customer-profile/**")
                                .addResourceLocations(configuredBaseDir + "customer-profile/", projectBaseDir + "customer-profile/");

                registry.addResourceHandler("/review-images/**")
                                .addResourceLocations(configuredBaseDir + "review-images/", projectBaseDir + "review-images/");

                registry.addResourceHandler("/seller_logos/**")
                                .addResourceLocations(configuredBaseDir + "seller_logos/", projectBaseDir + "seller_logos/");

                registry.addResourceHandler("/campaign-images/**")
                                .addResourceLocations(configuredBaseDir + "campaign-images/", projectBaseDir + "campaign-images/");

                registry.addResourceHandler("/banners/**")
                                .addResourceLocations(configuredBaseDir + "banners/", projectBaseDir + "banners/");
        }

}
