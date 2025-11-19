package com.example.jhapcham.order;

import jakarta.persistence.*;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDateTime;

@Entity
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class OrderTracking {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnore
    private Order order;

    @Enumerated(EnumType.STRING)
    private OrderTrackingStage stage;

    private String message;

    @Enumerated(EnumType.STRING)
    private BranchName branch;

    private LocalDateTime updateTime = LocalDateTime.now();
}
