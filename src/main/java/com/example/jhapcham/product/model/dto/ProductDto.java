package com.example.jhapcham.product.model.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
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

    private Long sellerId;

    @Min(0)
    private Integer stock;

    private MultipartFile image;

    private List<String> colors;

    private List<MultipartFile> additionalImages;

    private LocalDate manufacturingDate;
    private LocalDate expiryDate;

    private String brand;

    private Long id;
    private String warranty;
    private Integer totalLikes;
    private String features;
    private String specifications;
    private List<String> storage;
    private Double averageRating;
    private Long reviewCount;

}
