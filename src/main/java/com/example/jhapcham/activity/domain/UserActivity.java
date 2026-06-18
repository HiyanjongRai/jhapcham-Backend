package com.example.jhapcham.activity.domain;


import com.example.jhapcham.activity.application.*;
import com.example.jhapcham.activity.domain.*;
import com.example.jhapcham.activity.persistence.*;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_activities", indexes = {
        @Index(name = "idx_user_activities_user_product_type", columnList = "user_id,product_id,activity_type"),
        @Index(name = "idx_user_activities_product_type", columnList = "product_id,activity_type"),
        @Index(name = "idx_user_activities_timestamp", columnList = "timestamp")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long productId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ActivityType activityType;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(columnDefinition = "TEXT")
    private String details;

    @PrePersist
    protected void onCreate() {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
    }
}
