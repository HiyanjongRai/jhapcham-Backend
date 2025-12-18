package com.example.jhapcham.seller;

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
