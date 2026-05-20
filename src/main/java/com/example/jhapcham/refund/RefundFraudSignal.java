package com.example.jhapcham.refund;

import com.example.jhapcham.user.model.User;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "refund_fraud_signals",
        indexes = {
                @Index(name = "idx_refund_fraud_request", columnList = "refund_request_id"),
                @Index(name = "idx_refund_fraud_user_created", columnList = "user_id,created_at")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefundFraudSignal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "refund_request_id", nullable = false)
    @JsonIgnore
    private RefundRequest refundRequest;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String signalType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RefundFraudSeverity severity;

    @Column(nullable = false)
    private int score;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;

    @CreationTimestamp
    @Column(nullable = false)
    private LocalDateTime createdAt;
}
