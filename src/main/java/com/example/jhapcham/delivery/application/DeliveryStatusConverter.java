package com.example.jhapcham.delivery.application;


import com.example.jhapcham.delivery.application.*;
import com.example.jhapcham.delivery.domain.*;
import com.example.jhapcham.delivery.dto.*;
import com.example.jhapcham.delivery.persistence.*;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class DeliveryStatusConverter implements AttributeConverter<DeliveryStatus, String> {

    @Override
    public String convertToDatabaseColumn(DeliveryStatus attribute) {
        return attribute != null ? attribute.name() : null;
    }

    @Override
    public DeliveryStatus convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return DeliveryStatus.CREATED;
        }

        return switch (dbData.trim().toUpperCase()) {
            case "ORDER_PLACED", "PACKED" -> DeliveryStatus.CREATED;
            default -> DeliveryStatus.valueOf(dbData.trim().toUpperCase());
        };
    }
}
