package com.example.jhapcham.refund;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class RefundAnalyticsDTO {
    private long totalRefundRequests;
    private long activeRefunds;
    private long refundedCount;
    private BigDecimal totalRefundedAmount;
    private Map<String, Long> countByReason;
    private Map<String, BigDecimal> amountByReason;
    private List<Map<String, Object>> dailyTrends;
    private List<Map<String, Object>> topRefundedProducts;
    private List<Map<String, Object>> sellerRefundRates;
    private List<Map<String, Object>> categoryRefunds;
}
