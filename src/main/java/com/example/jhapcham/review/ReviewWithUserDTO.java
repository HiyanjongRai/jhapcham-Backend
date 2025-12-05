package com.example.jhapcham.review;

import java.util.List;

public record ReviewWithUserDTO(
        Long id,
        Integer rating,
        String comment,
        List<String> images,
        String reviewerName,
        String reviewerProfileImage
) {}
