package com.example.jhapcham.review;

import java.util.List;

public record ReviewSimpleDTO(
        Long id,
        Integer rating,
        String comment,
        List<String> images
) {}
