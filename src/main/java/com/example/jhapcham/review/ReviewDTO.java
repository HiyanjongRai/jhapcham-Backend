package com.example.jhapcham.review;

import java.util.List;

public record ReviewDTO(
        Long id,
        Integer rating,
        String comment,
        List<String> images,
        Long productId,
        String productName,
        Long orderId
) {}
