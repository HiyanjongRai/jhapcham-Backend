package com.example.jhapcham.review;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class ReviewResponseDTO {
    private Long id;
    private String userName;
    private String userProfileImage; // Optional, useful for UI
    private Long productId;
    private String productName; // Useful for "My Reviews"
    private Integer rating;
    private String comment;
    private String imagePath;
    private LocalDateTime createdAt;
}
