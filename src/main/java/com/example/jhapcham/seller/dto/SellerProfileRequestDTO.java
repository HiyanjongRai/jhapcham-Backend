package com.example.jhapcham.seller.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class SellerProfileRequestDTO {
    private String storeName;
    private String address;
    private String about;
    private String description;
    private MultipartFile logoImage; // Optional
}
