package com.example.jhapcham.notification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SmsPreferenceDTO {
    private Long id;
    private Boolean orderConfirmation;
    private Boolean shipmentUpdates;
    private Boolean deliveryNotifications;
    private Boolean refundAlerts;
    private Boolean disputeAlerts;
    private Boolean promotionalSms;
    private Boolean inventoryAlerts;
    private Boolean allSmsEnabled;
}
