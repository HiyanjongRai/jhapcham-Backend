package com.example.jhapcham.report.domain;

import com.example.jhapcham.user.domain.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "report_moderation_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportModerationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id")
    private Long id;

    @Column(name = "report_type", nullable = false, length = 30)
    private String reportType; // e.g. "PRODUCT", "SELLER", "CUSTOMER"

    @Column(name = "report_id", nullable = false)
    private Long reportId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "moderator_id")
    private User moderator;

    @Column(nullable = false, length = 50)
    private String action; // e.g. "ASSIGNED", "ACTION_TAKEN"

    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(nullable = false)
    private LocalDateTime timestamp;
}
