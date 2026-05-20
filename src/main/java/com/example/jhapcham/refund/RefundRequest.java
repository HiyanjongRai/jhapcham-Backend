package com.example.jhapcham.refund;

import com.example.jhapcham.order.Order;
import com.example.jhapcham.order.PaymentMethod;
import com.example.jhapcham.user.model.User;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "refund_requests",
        indexes = {
                @Index(name = "idx_refund_requests_customer_created", columnList = "customer_id,created_at"),
                @Index(name = "idx_refund_requests_seller_status", columnList = "seller_id,status"),
                @Index(name = "idx_refund_requests_status_created", columnList = "status,created_at"),
                @Index(name = "idx_refund_requests_order", columnList = "order_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_refund_customer_idempotency", columnNames = {"customer_id", "idempotency_key"})
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefundRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    @JsonIgnore
    private Order order;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RefundReason reason;

    @Enumerated(EnumType.STRING)
    @Column(length = 40)
    private RefundType refundType;

    @Column(columnDefinition = "TEXT")
    private String reasonDetails;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private RefundStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private PaymentMethod paymentMethod;

    @Column(length = 120)
    private String idempotencyKey;

    @Column(nullable = false, precision = 38, scale = 2)
    private BigDecimal itemSubtotal;

    @Column(nullable = false, precision = 38, scale = 2)
    private BigDecimal taxRefund;

    @Column(nullable = false, precision = 38, scale = 2)
    private BigDecimal shippingRefund;

    @Column(nullable = false, precision = 38, scale = 2)
    private BigDecimal discountAdjustment;

    @Column(nullable = false, precision = 38, scale = 2)
    private BigDecimal totalRefund;

    @Column(nullable = false, precision = 38, scale = 2)
    private BigDecimal sellerCommissionReversal;

    @Column(nullable = false)
    private boolean shippingIncluded;

    @Column(nullable = false)
    private int fraudScore;

    @Column(nullable = false)
    private boolean fraudFlagged;

    @Column(columnDefinition = "TEXT")
    private String customerNotes;

    @Column(columnDefinition = "TEXT")
    private String sellerNotes;

    @Column(columnDefinition = "TEXT")
    private String adminNotes;

    @Column(nullable = false)
    private boolean deleted;

    @CreationTimestamp
    @Column(nullable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    private LocalDateTime submittedAt;
    private LocalDateTime reviewedAt;
    private LocalDateTime approvedAt;
    private LocalDateTime refundedAt;
    private LocalDateTime cancelledAt;

    @OneToMany(mappedBy = "refundRequest", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<RefundLineItem> lineItems = new ArrayList<>();

    @OneToMany(mappedBy = "refundRequest", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<RefundEvidence> evidence = new ArrayList<>();

    @OneToMany(mappedBy = "refundRequest", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<RefundStatusHistory> statusHistory = new ArrayList<>();

    @OneToMany(mappedBy = "refundRequest", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<RefundTransaction> transactions = new ArrayList<>();

    @OneToMany(mappedBy = "refundRequest", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<RefundFraudSignal> fraudSignals = new ArrayList<>();

    public void addLineItem(RefundLineItem lineItem) {
        lineItems.add(lineItem);
        lineItem.setRefundRequest(this);
    }

    public void addEvidence(RefundEvidence item) {
        evidence.add(item);
        item.setRefundRequest(this);
    }

    public void addHistory(RefundStatusHistory item) {
        statusHistory.add(item);
        item.setRefundRequest(this);
    }

    public void addTransaction(RefundTransaction item) {
        transactions.add(item);
        item.setRefundRequest(this);
    }

    public void addFraudSignal(RefundFraudSignal item) {
        fraudSignals.add(item);
        item.setRefundRequest(this);
    }
}
