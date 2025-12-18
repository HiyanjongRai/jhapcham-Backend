package com.example.jhapcham.admin;

import com.example.jhapcham.user.model.Role;
import com.example.jhapcham.user.model.Status;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SellerAdminDetailDTO {
    private Long id;
    private String username;
    private String fullName;
    private String email;
    private String contactNumber;
    private Role role;
    private Status status;
    private String storeName;
    private int totalOrders;
    private int totalProducts;
    // Add income if needed
}
