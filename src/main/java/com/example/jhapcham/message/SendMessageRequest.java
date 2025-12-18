package com.example.jhapcham.message;

import lombok.Data;

@Data
public class SendMessageRequest {
    private Long receiverId;
    private Long productId; // Optional
    private String content;
}
