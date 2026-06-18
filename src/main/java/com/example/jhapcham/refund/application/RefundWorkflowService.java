package com.example.jhapcham.refund.application;

import com.example.jhapcham.Error.AuthenticationException; // wait, let's make sure it matches existing exceptions or generic ones
import com.example.jhapcham.refund.domain.Refund;
import com.example.jhapcham.refund.domain.RefundStatus;
import com.example.jhapcham.refund.domain.RefundType;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class RefundWorkflowService {

    private static final Map<RefundStatus, Set<RefundStatus>> ALLOWED_TRANSITIONS = new EnumMap<>(RefundStatus.class);

    static {
        // REQUEST_CREATED transitions
        ALLOWED_TRANSITIONS.put(RefundStatus.REQUEST_CREATED, Set.of(RefundStatus.UNDER_REVIEW));

        // UNDER_REVIEW transitions
        ALLOWED_TRANSITIONS.put(RefundStatus.UNDER_REVIEW, Set.of(
                RefundStatus.MORE_EVIDENCE_REQUESTED,
                RefundStatus.OFFER_MADE,
                RefundStatus.SELLER_APPROVED,
                RefundStatus.SELLER_REJECTED,
                RefundStatus.CLOSED,
                RefundStatus.CUSTOMER_ACCEPTS,
                RefundStatus.ADMIN_REVIEW
        ));

        // MORE_EVIDENCE_REQUESTED transitions
        ALLOWED_TRANSITIONS.put(RefundStatus.MORE_EVIDENCE_REQUESTED, Set.of(
                RefundStatus.UNDER_REVIEW,
                RefundStatus.SELLER_APPROVED,
                RefundStatus.SELLER_REJECTED,
                RefundStatus.OFFER_MADE,
                RefundStatus.CLOSED,
                RefundStatus.ADMIN_REVIEW
        ));

        // OFFER_MADE transitions
        ALLOWED_TRANSITIONS.put(RefundStatus.OFFER_MADE, Set.of(
                RefundStatus.CUSTOMER_ACCEPTS,
                RefundStatus.UNDER_REVIEW,
                RefundStatus.ADMIN_REVIEW,
                RefundStatus.SELLER_APPROVED,
                RefundStatus.SELLER_REJECTED,
                RefundStatus.OFFER_MADE,
                RefundStatus.CLOSED
        ));

        // SELLER_APPROVED transitions
        ALLOWED_TRANSITIONS.put(RefundStatus.SELLER_APPROVED, Set.of(RefundStatus.RETURN_PENDING, RefundStatus.REFUND_PROCESSING, RefundStatus.REPLACEMENT_PREPARING));

        // RETURN_PENDING transitions
        ALLOWED_TRANSITIONS.put(RefundStatus.RETURN_PENDING, Set.of(RefundStatus.RETURN_SHIPPED));

        // RETURN_SHIPPED transitions
        ALLOWED_TRANSITIONS.put(RefundStatus.RETURN_SHIPPED, Set.of(RefundStatus.RETURN_RECEIVED));

        // RETURN_RECEIVED transitions
        ALLOWED_TRANSITIONS.put(RefundStatus.RETURN_RECEIVED, Set.of(RefundStatus.PRODUCT_INSPECTION));

        // PRODUCT_INSPECTION transitions
        ALLOWED_TRANSITIONS.put(RefundStatus.PRODUCT_INSPECTION, Set.of(RefundStatus.INSPECTION_COMPLETE));

        // INSPECTION_COMPLETE transitions
        ALLOWED_TRANSITIONS.put(RefundStatus.INSPECTION_COMPLETE, Set.of(
                RefundStatus.REFUND_PROCESSING,
                RefundStatus.REFUND_COMPLETED,
                RefundStatus.REPLACEMENT_PREPARING,
                RefundStatus.OFFER_MADE,
                RefundStatus.SELLER_REJECTED
        ));

        // REFUND_PROCESSING transitions
        ALLOWED_TRANSITIONS.put(RefundStatus.REFUND_PROCESSING, Set.of(RefundStatus.PENDING_ADMIN_VERIFICATION, RefundStatus.REFUND_COMPLETED));

        // PENDING_ADMIN_VERIFICATION transitions
        ALLOWED_TRANSITIONS.put(RefundStatus.PENDING_ADMIN_VERIFICATION, Set.of(RefundStatus.REFUND_COMPLETED, RefundStatus.REFUND_PROCESSING));

        // REPLACEMENT_PREPARING transitions
        ALLOWED_TRANSITIONS.put(RefundStatus.REPLACEMENT_PREPARING, Set.of(RefundStatus.REPLACEMENT_SHIPPED));

        // REPLACEMENT_SHIPPED transitions
        ALLOWED_TRANSITIONS.put(RefundStatus.REPLACEMENT_SHIPPED, Set.of(RefundStatus.EXCHANGE_COMPLETED, RefundStatus.UNDER_REVIEW));

        // SELLER_REJECTED transitions
        ALLOWED_TRANSITIONS.put(RefundStatus.SELLER_REJECTED, Set.of(
                RefundStatus.CUSTOMER_ACCEPTS,
                RefundStatus.ADMIN_REVIEW
        ));

        // CUSTOMER_ACCEPTS transitions
        ALLOWED_TRANSITIONS.put(RefundStatus.CUSTOMER_ACCEPTS, Set.of(
                RefundStatus.CLOSED,
                RefundStatus.REFUND_PROCESSING,
                RefundStatus.REPLACEMENT_PREPARING
        ));

        // ADMIN_REVIEW transitions
        ALLOWED_TRANSITIONS.put(RefundStatus.ADMIN_REVIEW, Set.of(
                RefundStatus.ADMIN_APPROVED_REFUND,
                RefundStatus.ADMIN_REJECTED_REFUND,
                RefundStatus.MORE_EVIDENCE_REQUESTED
        ));

        // ADMIN_APPROVED_REFUND transitions
        ALLOWED_TRANSITIONS.put(RefundStatus.ADMIN_APPROVED_REFUND, Set.of(RefundStatus.RETURN_PENDING, RefundStatus.REFUND_PROCESSING, RefundStatus.REPLACEMENT_PREPARING));

        // ADMIN_REJECTED_REFUND transitions
        ALLOWED_TRANSITIONS.put(RefundStatus.ADMIN_REJECTED_REFUND, Set.of(RefundStatus.CLOSED));
    }

    public void validateTransition(RefundStatus current, RefundStatus next) {
        if (current == next) {
            return;
        }
        Set<RefundStatus> allowed = ALLOWED_TRANSITIONS.get(current);
        if (allowed == null || !allowed.contains(next)) {
            throw new IllegalStateException("Invalid state transition from " + current + " to " + next);
        }
    }
}
