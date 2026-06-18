package com.example.jhapcham.refund.domain;

import com.example.jhapcham.order.domain.Order;
import com.example.jhapcham.user.domain.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "refunds")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Refund {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(name = "refund_number", unique = true, nullable = false, length = 50)
    private String refundNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private User customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id")
    private User seller;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RefundType type;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 60)
    private RefundStatus status;

    @NotBlank
    @Column(nullable = false, length = 255)
    private String reason;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(length = 100)
    private InspectionVerdict verdict;

    @Column(name = "refund_amount", precision = 12, scale = 2)
    private BigDecimal refundAmount;

    @Column(name = "damage_score")
    private Integer damageScore;

    @Column(name = "inspection_notes", columnDefinition = "TEXT")
    private String inspectionNotes;

    @Column(name = "tracking_number", length = 150)
    private String trackingNumber;

    @Column(name = "admin_decision", length = 100)
    private String adminDecision;

    @Column(name = "return_required")
    private Boolean returnRequired;

    @Column(name = "payment_proof_url", length = 500)
    private String paymentProofUrl;

    @Column(name = "payment_reference", length = 150)
    private String paymentReference;

    @Column(name = "payment_comment", columnDefinition = "TEXT")
    private String paymentComment;

    @Column(name = "customer_qr_url", length = 500)
    private String customerQrUrl;

    @Column(name = "customer_account_details", columnDefinition = "TEXT")
    private String customerAccountDetails;

    @Column(name = "replacement_courier", length = 150)
    private String replacementCourier;

    @Column(name = "replacement_tracking_number", length = 150)
    private String replacementTrackingNumber;

    @Column(name = "replacement_shipped_at")
    private LocalDateTime replacementShippedAt;

    @OneToMany(mappedBy = "refund", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<RefundEvidence> evidence = new ArrayList<>();

    @OneToOne(mappedBy = "refund", cascade = CascadeType.ALL, orphanRemoval = true)
    private RefundInspection inspection;

    @OneToMany(mappedBy = "refund", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<RefundAuditLog> auditLogs = new ArrayList<>();

    @OneToMany(mappedBy = "refund", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<RefundItem> items = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public void addEvidence(RefundEvidence item) {
        evidence.add(item);
        item.setRefund(this);
    }

    public void addAuditLog(RefundAuditLog log) {
        auditLogs.add(log);
        log.setRefund(this);
    }

    public void addRefundItem(RefundItem item) {
        items.add(item);
        item.setRefund(this);
    }
}
