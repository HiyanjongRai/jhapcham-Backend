package com.example.jhapcham.seller.model;

import com.example.jhapcham.user.model.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "seller_applications")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SellerApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // The seller user who applied
    @OneToOne(optional = false)
    @JoinColumn(name = "user_id", unique = true)
    private User user;

    // Basic store info (what admin sees)
    @Column(nullable = false)
    private String storeName;

    @Column(nullable = false)
    private String address;

    // Uploaded document paths (stored on disk or S3)
    private String idDocumentPath;
    private String businessLicensePath;
    private String taxCertificatePath;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApplicationStatus status;

    private String reviewNote;         // optional: reason / notes by admin
    private LocalDateTime submittedAt; // when seller applied
    private LocalDateTime reviewedAt;  // when admin approved/rejected
}
