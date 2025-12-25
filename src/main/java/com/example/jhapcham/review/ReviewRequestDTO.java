package com.example.jhapcham.review;

import lombok.Data;

@Data
public class ReviewRequestDTO {
    private Long productId;
    private Integer rating;
    private String comment;
}
