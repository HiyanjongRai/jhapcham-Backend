package com.example.jhapcham.review;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class ReviewRequestDTO {
    private Long productId;
    private Integer rating;
    private String comment;
}
