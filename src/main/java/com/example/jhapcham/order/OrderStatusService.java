package com.example.jhapcham.order;

import com.example.jhapcham.Error.BusinessValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
public class OrderStatusService {

    private static final Map<OrderStatus, Set<OrderStatus>> VALID_TRANSITIONS = new HashMap<>();

    static {
        VALID_TRANSITIONS.put(OrderStatus.DRAFT, EnumSet.of(
                OrderStatus.CONFIRMED,
                OrderStatus.CANCELLED,
                OrderStatus.FAILED));
        VALID_TRANSITIONS.put(OrderStatus.PENDING, EnumSet.of(
                OrderStatus.COD_PENDING,
                OrderStatus.CONFIRMED,
                OrderStatus.CONFIRMED_BY_CALL,
                OrderStatus.PROCESSING,
                OrderStatus.PACKED,
                OrderStatus.SHIPPED,
                OrderStatus.OUT_FOR_DELIVERY,
                OrderStatus.CANCELLED,
                OrderStatus.FAILED));
        VALID_TRANSITIONS.put(OrderStatus.COD_PENDING, EnumSet.of(
                OrderStatus.CONFIRMED_BY_CALL,
                OrderStatus.CONFIRMED,
                OrderStatus.PROCESSING,
                OrderStatus.PACKED,
                OrderStatus.SHIPPED,
                OrderStatus.OUT_FOR_DELIVERY,
                OrderStatus.CANCELLED,
                OrderStatus.FAILED));
        VALID_TRANSITIONS.put(OrderStatus.CONFIRMED, EnumSet.of(
                OrderStatus.PROCESSING,
                OrderStatus.PACKED,
                OrderStatus.SHIPPED,
                OrderStatus.OUT_FOR_DELIVERY,
                OrderStatus.CANCELLED,
                OrderStatus.FAILED));
        VALID_TRANSITIONS.put(OrderStatus.CONFIRMED_BY_CALL, EnumSet.of(
                OrderStatus.PROCESSING,
                OrderStatus.PACKED,
                OrderStatus.SHIPPED,
                OrderStatus.CANCELLED));
        VALID_TRANSITIONS.put(OrderStatus.PROCESSING, EnumSet.of(
                OrderStatus.PACKED,
                OrderStatus.SHIPPED,
                OrderStatus.OUT_FOR_DELIVERY,
                OrderStatus.CANCELLED,
                OrderStatus.FAILED));
        VALID_TRANSITIONS.put(OrderStatus.PACKED, EnumSet.of(
                OrderStatus.SHIPPED,
                OrderStatus.OUT_FOR_DELIVERY,
                OrderStatus.CANCELLED));
        VALID_TRANSITIONS.put(OrderStatus.SHIPPED, EnumSet.of(
                OrderStatus.OUT_FOR_DELIVERY,
                OrderStatus.DELIVERED,
                OrderStatus.RETURN_REQUESTED,
                OrderStatus.RETURNED,
                OrderStatus.CANCELLED,
                OrderStatus.FAILED));
        VALID_TRANSITIONS.put(OrderStatus.OUT_FOR_DELIVERY, EnumSet.of(
                OrderStatus.DELIVERED,
                OrderStatus.RETURN_REQUESTED,
                OrderStatus.RETURNED,
                OrderStatus.CANCELLED,
                OrderStatus.FAILED));
        VALID_TRANSITIONS.put(OrderStatus.RETURN_REQUESTED, EnumSet.of(
                OrderStatus.RETURNED,
                OrderStatus.REFUNDED));
        VALID_TRANSITIONS.put(OrderStatus.RETURNED, EnumSet.of(OrderStatus.REFUNDED));
    }

    public void validateTransition(OrderStatus from, OrderStatus to) {
        if (from == to)
            return;

        Set<OrderStatus> allowed = VALID_TRANSITIONS.get(from);
        if (allowed == null || !allowed.contains(to)) {
            log.error("Invalid status transition: {} -> {}", from, to);
            throw new BusinessValidationException("Cannot transition order from " + from + " to " + to);
        }
    }

    public boolean canCancel(OrderStatus currentStatus) {
        return currentStatus == OrderStatus.DRAFT ||
                currentStatus == OrderStatus.PENDING ||
                currentStatus == OrderStatus.COD_PENDING ||
                currentStatus == OrderStatus.CONFIRMED ||
                currentStatus == OrderStatus.CONFIRMED_BY_CALL ||
                currentStatus == OrderStatus.PROCESSING ||
                currentStatus == OrderStatus.PACKED ||
                currentStatus == OrderStatus.SHIPPED ||
                currentStatus == OrderStatus.OUT_FOR_DELIVERY;
    }
}
