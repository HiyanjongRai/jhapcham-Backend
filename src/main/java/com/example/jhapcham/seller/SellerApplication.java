package com.example.jhapcham.seller;

import com.example.jhapcham.user.model.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "seller_applications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SellerApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // The seller user who applied
    @OneToOne(optional = false)
    @JoinColumn(name = "user_id", unique = true, nullable = false)
    private User user;

    // Basic store info that admin sees
    @Column(nullable = false)
    private String storeName;

    @Column(nullable = false)
    private String address;

    // Uploaded document paths
    @Column(name = "id_document_path")
    private String idDocumentPath;

    @Column(name = "business_license_path")
    private String businessLicensePath;

    @Column(name = "tax_certificate_path")
    private String taxCertificatePath;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ApplicationStatus status;

    // Optional review info
    @Column(columnDefinition = "TEXT")
    private String reviewNote;

    private LocalDateTime submittedAt;
    private LocalDateTime reviewedAt;


}