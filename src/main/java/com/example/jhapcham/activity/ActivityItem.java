// src/main/java/com/example/jhapcham/activity/ActivityItem.java
package com.example.jhapcham.activity;

import lombok.*;
import java.time.Instant;

@Getter @Setter @Builder
@AllArgsConstructor @NoArgsConstructor
public class ActivityItem {
    private ActivityType type;
    private Instant occurredAt;

    private Long productId;
    private String productName;
    private String category;

    private String text;        // comment text
    private Integer quantity;   // cart/order qty
    private Double amount;      // order line/total
    private Double stars;       // rating value
}
