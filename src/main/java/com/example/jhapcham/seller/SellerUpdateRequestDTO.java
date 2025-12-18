package com.example.jhapcham.seller;

import org.springframework.web.multipart.MultipartFile;

public record SellerUpdateRequestDTO(
        String storeName,
        String address,
        String description,
        String about,
        Double insideValleyDeliveryFee,
        Double outsideValleyDeliveryFee,
        Boolean freeShippingEnabled,
        Double freeShippingMinOrder,
        String contactNumber,
        MultipartFile logoImage
) {}
