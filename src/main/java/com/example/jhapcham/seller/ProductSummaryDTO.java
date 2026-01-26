package com.example.jhapcham.seller;

import com.example.jhapcham.product.Product;
import com.example.jhapcham.product.ProductImage;
import lombok.Builder;
import lombok.Data;

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
                .mainImage(
                        p.getImages() != null && !p.getImages().isEmpty()
                                ? p.getImages().stream().findFirst().map(ProductImage::getImagePath).orElse(null)
                                : null)
                .build();
    }
}
