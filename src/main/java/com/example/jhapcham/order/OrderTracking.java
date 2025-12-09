package com.example.jhapcham.order;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderTracking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    @Enumerated(EnumType.STRING)
    private OrderTrackingStage stage;

    private String message;

    @Enumerated(EnumType.STRING)
    private BranchName branch;

    @Builder.Default
    private LocalDateTime updateTime = LocalDateTime.now();


}