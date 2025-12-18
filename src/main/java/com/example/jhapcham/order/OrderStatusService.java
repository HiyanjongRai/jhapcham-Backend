package com.example.jhapcham.order;

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
        VALID_TRANSITIONS.put(OrderStatus.NEW, EnumSet.of(OrderStatus.PROCESSING, OrderStatus.CANCELED));
        VALID_TRANSITIONS.put(OrderStatus.PROCESSING, EnumSet.of(OrderStatus.SHIPPED_TO_BRANCH, OrderStatus.CANCELED));
        VALID_TRANSITIONS.put(OrderStatus.SHIPPED_TO_BRANCH, EnumSet.of(OrderStatus.OUT_FOR_DELIVERY));
        VALID_TRANSITIONS.put(OrderStatus.OUT_FOR_DELIVERY, EnumSet.of(OrderStatus.DELIVERED, OrderStatus.CANCELED));
        // DELIVERED and CANCELED are terminal states in this simple model
    }

    public void validateTransition(OrderStatus from, OrderStatus to) {
        if (from == to)
            return;

        Set<OrderStatus> allowed = VALID_TRANSITIONS.get(from);
        if (allowed == null || !allowed.contains(to)) {
            log.error("Invalid status transition: {} -> {}", from, to);
            throw new RuntimeException("Cannot transition order from " + from + " to " + to);
        }
    }

    public boolean canCancel(OrderStatus currentStatus) {
        return currentStatus == OrderStatus.NEW ||
                currentStatus == OrderStatus.PROCESSING ||
                currentStatus == OrderStatus.OUT_FOR_DELIVERY; // Customers/Sellers might cancel until last mile
    }
}
