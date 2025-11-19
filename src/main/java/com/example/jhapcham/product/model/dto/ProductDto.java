package com.example.jhapcham.product.model.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductDto {

    @NotBlank
    private String name;

    @Size(max = 10_000)
    private String description;

    @Size(max = 300)
    private String shortDescription;

    @NotNull
    @Positive
    private Double price;

    @NotBlank
    private String category;

    @Size(max = 20_000)
    private String others;

    @Min(0)
    private Integer stock;

    private MultipartFile image;

    private List<String> colors;

    private String brand;

    private Long id;

    private Integer totalLikes;

    // NEW FIELDS
    private Double averageRating;
    private Long reviewCount;
}
