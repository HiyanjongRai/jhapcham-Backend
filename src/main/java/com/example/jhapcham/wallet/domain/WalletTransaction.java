package com.example.jhapcham.wallet.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Audit trail for every wallet credit or debit.
 * Linked to optional refund when credited from a refund workflow.
 */
@Entity
@Table(name = "wallet_transactions",
       indexes = {
           @Index(name = "idx_wallet_txn_wallet_id",  columnList = "wallet_id"),
           @Index(name = "idx_wallet_txn_refund_id",  columnList = "refund_id"),
           @Index(name = "idx_wallet_txn_created_at", columnList = "wallet_id,created_at")
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WalletTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "wallet_id", nullable = false)
    private Wallet wallet;

    /** Set when this transaction is linked to a refund approval. */
    @Column(name = "refund_id")
    private Long refundId;

    /** CREDIT: money added; DEBIT: money removed. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TransactionType type;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    /** Balance AFTER this transaction was applied. */
    @Column(name = "balance_after", nullable = false, precision = 12, scale = 2)
    private BigDecimal balanceAfter;

    @Column(length = 500)
    private String description;

    /** SYSTEM, ADMIN, or CUSTOMER — who initiated this transaction. */
    @Column(name = "created_by", length = 30)
    private String createdBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public enum TransactionType {
        CREDIT, DEBIT
    }
}
