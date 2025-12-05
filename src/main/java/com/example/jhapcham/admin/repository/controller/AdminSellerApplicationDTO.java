package com.example.jhapcham.admin.repository.controller;

import com.example.jhapcham.seller.model.ApplicationStatus;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdminSellerApplicationDTO {
    private Long applicationId;
    private Long userId;
    private String username;
    private ApplicationStatus status;
    private String storeName;
    private String address;

    // File URLs
    private String idDocumentUrl;
    private String businessLicenseUrl;
    private String taxCertificateUrl;
    private String logoImageUrl;
}
