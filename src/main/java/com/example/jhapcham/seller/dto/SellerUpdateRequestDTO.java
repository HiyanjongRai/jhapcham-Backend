package com.example.jhapcham.seller.dto;


import com.example.jhapcham.seller.application.*;
import com.example.jhapcham.seller.domain.*;
import com.example.jhapcham.seller.dto.*;
import com.example.jhapcham.seller.persistence.*;
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
