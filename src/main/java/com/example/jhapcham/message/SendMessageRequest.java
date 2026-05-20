package com.example.jhapcham.message;

import lombok.Data;

@Data
public class SendMessageRequest {
    private Long receiverId;
    private Long productId;     // Optional — general product inquiry
    private Long disputeId;     // Optional — case-linked dispute message (Gap 1)
    private Long refundRequestId; // Optional — case-linked refund message (Gap 1)
    private String content;
}
