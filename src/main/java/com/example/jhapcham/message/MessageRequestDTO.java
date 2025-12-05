package com.example.jhapcham.message;

import lombok.Data;

@Data
public class MessageRequestDTO {
    private Long senderId;
    private Long receiverId;
    private Long productId; // optional for product inquiry
    private String content;
}
