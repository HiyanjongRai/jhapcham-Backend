package com.example.jhapcham.message.dto;

import lombok.Data;

@Data
public class SendMessageRequest {
    private Long receiverId;
    private Long productId;
    private String content;
}
