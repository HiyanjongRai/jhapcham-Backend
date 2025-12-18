package com.example.jhapcham.product;

import org.springframework.web.multipart.MultipartFile;
import java.math.BigDecimal;

public record ProductCreateRequestDTO(
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
                Integer stockQuantity,
                Integer warrantyMonths,
                String manufactureDate,
                String expiryDate,
                MultipartFile[] images,
                Boolean freeShipping,
                Double insideValleyShipping,
                Double outsideValleyShipping,
                Double sellerFreeShippingMinOrder) {
}
