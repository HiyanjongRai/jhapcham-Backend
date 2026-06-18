package com.example.jhapcham.notification.dto;


import com.example.jhapcham.notification.application.*;
import com.example.jhapcham.notification.domain.*;
import com.example.jhapcham.notification.dto.*;
import com.example.jhapcham.notification.persistence.*;
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
    private Boolean promotionalSms;
    private Boolean inventoryAlerts;
    private Boolean allSmsEnabled;
}
