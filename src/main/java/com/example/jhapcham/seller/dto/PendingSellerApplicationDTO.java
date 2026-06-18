package com.example.jhapcham.seller.dto;


import com.example.jhapcham.seller.application.*;
import com.example.jhapcham.seller.domain.*;
import com.example.jhapcham.seller.dto.*;
import com.example.jhapcham.seller.persistence.*;
import java.time.LocalDateTime;

public record PendingSellerApplicationDTO(
        Long applicationId,
        Long userId,
        String username,
        String email,
        String storeName,
        String address,
        String status,
        LocalDateTime submittedAt,
        String idDocumentPath,
        String businessLicensePath,
        String taxCertificatePath
) {
}
