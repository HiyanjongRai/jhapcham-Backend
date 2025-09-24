package com.example.jhapcham.product.model.dto;

import lombok.*;
import org.springframework.web.multipart.MultipartFile;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductDto {
    private String name;
    private String description;
    private Double price;
    private String category;
    private MultipartFile image; // image file uploaded
}