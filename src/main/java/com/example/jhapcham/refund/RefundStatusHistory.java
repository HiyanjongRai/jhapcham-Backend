package com.example.jhapcham.refund;

import com.example.jhapcham.user.model.User;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "refund_status_history",
        indexes = {
                @Index(name = "idx_refund_status_history_request_created", columnList = "refund_request_id,created_at")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefundStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "refund_request_id", nullable = false)
    @JsonIgnore
    private RefundRequest refundRequest;

    @Enumerated(EnumType.STRING)
    private RefundStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RefundStatus toStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id")
    private User actor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RefundActorType actorType;

    @Column(columnDefinition = "TEXT")
    private String note;

    @CreationTimestamp
    @Column(nullable = false)
    private LocalDateTime createdAt;
}
