package com.example.jhapcham.seller.dto;

import com.example.jhapcham.product.model.Product;
import com.example.jhapcham.user.model.Role;
import com.example.jhapcham.user.model.Status;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class SellerProfileResponseDTO {
    private Long userId;
    private String username;
    private String fullName;
    private String email;
    private String contactNumber;
    private String profileImagePath;
    private Role role;
    private Status status;

    private String storeName;
    private String address;
    private String about;
    private String description;
    private Boolean isVerified;
    private LocalDateTime joinedDate;
    private String logoImagePath;

    private List<Product> products;
}
