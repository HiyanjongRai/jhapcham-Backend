package com.example.jhapcham.message;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class MessageDTO {
    private Long id;
    private Long senderId;
    private String senderName;
    private String senderProfileImage;
    private Long receiverId;
    private String receiverName;
    private String content;
    private Long productId;
    private String productName;
    private String productImage;
    private LocalDateTime createdAt;
    private boolean isRead;
}
