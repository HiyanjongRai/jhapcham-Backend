package com.example.jhapcham.product.model.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import org.springframework.web.multipart.MultipartFile;
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductDto {

    @NotBlank
    private String name;

    @Size(max = 10_000)
    private String description;

    @Size(max = 300)                 // NEW
    private String shortDescription; // NEW

    @NotNull @Positive
    private Double price;

    @NotBlank
    private String category;

    @Size(max = 20_000)
    private String others;

    @Min(0)
    private Integer stock;

    private MultipartFile image;

    // legacy/response helpers
    private Long id;
    private Integer totalLikes;
}
