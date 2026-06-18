package com.example.jhapcham.seller.dto;


import com.example.jhapcham.seller.application.*;
import com.example.jhapcham.seller.domain.*;
import com.example.jhapcham.seller.dto.*;
import com.example.jhapcham.seller.persistence.*;
import com.example.jhapcham.product.domain.Product;
import lombok.Builder;
import lombok.Data;
import org.hibernate.Hibernate;

@Data
@Builder
public class ProductSummaryDTO {

    private Long productId;
    private String name;
    private String category;
    private String brand;
    private String mainImage;
    private Double price;

    public static ProductSummaryDTO from(Product p) {
        return ProductSummaryDTO.builder()
                .productId(p.getId())
                .name(p.getName())
                .category(p.getCategory())
                .brand(p.getBrand())
                .price(p.getPrice().doubleValue())
                .mainImage(resolveMainImage(p))
                .build();
    }

    private static String resolveMainImage(Product p) {
        if (p.getImages() == null || !Hibernate.isInitialized(p.getImages()) || p.getImages().isEmpty()) {
            return null;
        }
        return p.getImages().get(0).getImagePath();
    }
}
