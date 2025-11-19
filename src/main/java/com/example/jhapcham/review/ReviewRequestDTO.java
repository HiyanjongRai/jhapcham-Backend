package com.example.jhapcham.review;

import lombok.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewRequestDTO {

    private Long orderId;

    private Integer rating;

    private String comment;

    private List<MultipartFile> images;
}
