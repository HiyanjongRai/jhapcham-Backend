package com.example.jhapcham.admin.dto;


import com.example.jhapcham.admin.application.*;
import com.example.jhapcham.admin.dto.*;
import com.example.jhapcham.user.domain.Role;
import com.example.jhapcham.user.domain.Status;
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
    private double totalIncome;
    private int totalDelivered;
    private String idDocumentPath;
    private String businessLicensePath;
    private String taxCertificatePath;
    private double totalCommission;

}
