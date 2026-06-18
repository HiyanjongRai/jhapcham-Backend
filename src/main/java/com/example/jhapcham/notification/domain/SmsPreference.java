package com.example.jhapcham.notification.domain;


import com.example.jhapcham.notification.application.*;
import com.example.jhapcham.notification.domain.*;
import com.example.jhapcham.notification.dto.*;
import com.example.jhapcham.notification.persistence.*;
import com.example.jhapcham.user.domain.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "sms_preferences")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SmsPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    @Builder.Default
    private Boolean orderConfirmation = true;

    @Column(nullable = false)
    @Builder.Default
    private Boolean shipmentUpdates = true;

    @Column(nullable = false)
    @Builder.Default
    private Boolean deliveryNotifications = true;

    @Column(nullable = false)
    @Builder.Default
    private Boolean promotionalSms = false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean inventoryAlerts = true;  // For sellers

    @Column(nullable = false)
    @Builder.Default
    private Boolean allSmsEnabled = true;

    @CreationTimestamp
    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public void updateAllSmsStatus(Boolean enabled) {
        this.allSmsEnabled = enabled;
        if (enabled) {
            this.orderConfirmation = true;
            this.shipmentUpdates = true;
            this.deliveryNotifications = true;
            this.inventoryAlerts = true;
        }
    }
}
