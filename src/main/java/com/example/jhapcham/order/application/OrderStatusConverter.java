package com.example.jhapcham.order.application;


import com.example.jhapcham.order.application.*;
import com.example.jhapcham.order.domain.*;
import com.example.jhapcham.order.dto.*;
import com.example.jhapcham.order.persistence.*;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class OrderStatusConverter implements AttributeConverter<OrderStatus, String> {

    @Override
    public String convertToDatabaseColumn(OrderStatus attribute) {
        return attribute != null ? attribute.name() : null;
    }

    @Override
    public OrderStatus convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return OrderStatus.PENDING;
        }

        return switch (dbData.trim().toUpperCase()) {
            case "NEW" -> OrderStatus.PENDING;
            case "SHIPPED_TO_BRANCH" -> OrderStatus.SHIPPED;
            case "CANCELED" -> OrderStatus.CANCELLED;
            case "PARTIALLY_REFUNDED" -> OrderStatus.REFUNDED;
            default -> OrderStatus.valueOf(dbData.trim().toUpperCase());
        };
    }
}
