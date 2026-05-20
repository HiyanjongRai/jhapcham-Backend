package com.example.jhapcham.refund;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RefundStatusTransitionPolicyTest {

    private final RefundStatusTransitionPolicy policy = new RefundStatusTransitionPolicy();

    @Test
    void allowsProductionRefundLifecycle() {
        assertThat(policy.isAllowed(null, RefundStatus.REQUESTED)).isTrue();
        assertThat(policy.isAllowed(RefundStatus.REQUESTED, RefundStatus.UNDER_REVIEW)).isTrue();
        assertThat(policy.isAllowed(RefundStatus.UNDER_REVIEW, RefundStatus.APPROVED)).isTrue();
        assertThat(policy.isAllowed(RefundStatus.APPROVED, RefundStatus.REFUND_PROCESSING)).isTrue();
        assertThat(policy.isAllowed(RefundStatus.REFUND_PROCESSING, RefundStatus.REFUNDED)).isTrue();
    }

    @Test
    void blocksIllegalAndTerminalTransitions() {
        assertThat(policy.isAllowed(RefundStatus.REQUESTED, RefundStatus.REFUNDED)).isFalse();
        assertThat(policy.isAllowed(RefundStatus.REFUNDED, RefundStatus.REQUESTED)).isFalse();
        assertThat(policy.isAllowed(RefundStatus.REJECTED, RefundStatus.APPROVED)).isFalse();
        assertThat(policy.isAllowed(RefundStatus.CANCELLED, RefundStatus.UNDER_REVIEW)).isFalse();
        assertThat(policy.isAllowed(RefundStatus.FAILED, RefundStatus.REFUND_PROCESSING)).isFalse();
    }
}
