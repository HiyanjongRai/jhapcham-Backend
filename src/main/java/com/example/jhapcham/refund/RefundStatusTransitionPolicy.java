package com.example.jhapcham.refund;

import org.springframework.stereotype.Component;

@Component
public class RefundStatusTransitionPolicy {
    public boolean isAllowed(RefundStatus from, RefundStatus to) {
        if (from == null) {
            return to == RefundStatus.REQUESTED;
        }
        if (from == to) {
            return true;
        }
        return switch (from) {
            case REQUESTED -> to == RefundStatus.UNDER_REVIEW || to == RefundStatus.CANCELLED || to == RefundStatus.REJECTED;
            case UNDER_REVIEW -> to == RefundStatus.APPROVED || to == RefundStatus.REJECTED || to == RefundStatus.CANCELLED;
            case APPROVED -> to == RefundStatus.REFUND_PROCESSING || to == RefundStatus.CANCELLED;
            case REFUND_PROCESSING -> to == RefundStatus.REFUNDED || to == RefundStatus.FAILED;
            case REFUNDED, REJECTED, CANCELLED, FAILED -> false;
        };
    }
}
