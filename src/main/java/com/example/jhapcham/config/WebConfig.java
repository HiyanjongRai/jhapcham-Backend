package com.example.jhapcham.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

@Configuration
public class WebConfig implements WebMvcConfigurer {

        private final String uploadDir;

        public WebConfig(@org.springframework.beans.factory.annotation.Value("${file.upload.dir:uploads}") String uploadDir) {
                this.uploadDir = Path.of(uploadDir).toAbsolutePath().normalize().toString();
        }

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

                registry.addResourceHandler("/product-images/**")
                                .addResourceLocations(uploadLocation("product-images"));

                registry.addResourceHandler("/products/**")
                                .addResourceLocations(uploadLocation("products"));

                registry.addResourceHandler("/customer-profile/**")
                                .addResourceLocations(uploadLocation("customer-profile"));

                registry.addResourceHandler("/review-images/**")
                                .addResourceLocations(uploadLocation("review-images"));

                registry.addResourceHandler("/seller_logos/**")
                                .addResourceLocations(uploadLocation("seller_logos"));

                registry.addResourceHandler("/campaign-images/**")
                                .addResourceLocations(uploadLocation("campaign-images"));

                registry.addResourceHandler("/banners/**")
                                .addResourceLocations(uploadLocation("banners"));

                registry.addResourceHandler("/refund_evidence/**")
                                .addResourceLocations(uploadLocation("refund_evidence"));

                registry.addResourceHandler("/dispute_evidence/**")
                                .addResourceLocations(uploadLocation("dispute_evidence"));

                registry.addResourceHandler("/reports/**")
                                .addResourceLocations(uploadLocation("reports"));
        }

        private String uploadLocation(String subdir) {
                return Path.of(uploadDir).resolve(subdir).normalize().toUri().toString();
        }

}
