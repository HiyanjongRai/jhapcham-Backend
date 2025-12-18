package com.example.jhapcham.product;

import org.springframework.web.multipart.MultipartFile;
import java.math.BigDecimal;

public record ProductUpdateRequestDTO(

                String name,
                String shortDescription,
                String description,
                String category,
                String brand,
                String specification,
                String storageSpec,
                String features,
                String colorOptions,

                BigDecimal price,
                Boolean onSale,
                BigDecimal discountPrice,
                BigDecimal salePercentage,

                Integer stockQuantity,
                Integer warrantyMonths,
                String manufactureDate,
                String expiryDate,

                MultipartFile[] newImages,

                // SHIPPING FIELDS
                Boolean freeShipping,
                Double insideValleyShipping,
                Double outsideValleyShipping,
                Double sellerFreeShippingMinOrder) {
}
